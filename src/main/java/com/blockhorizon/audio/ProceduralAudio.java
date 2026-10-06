package com.blockhorizon.audio;

import com.blockhorizon.world.BlockType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProceduralAudio implements AutoCloseable {
    private static final int SAMPLE_RATE = 22_050;
    private final ExecutorService audioThread = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "blockhorizon-audio");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean enabled = true;
    private volatile float volume = 0.34f;

    public void breakBlock(BlockType type) {
        float pitch = switch (type) {
            case STONE, BRICK, BEDROCK -> 115f;
            case WOOD, PLANKS, CHEST -> 155f;
            case CRYSTAL, GLOWSTONE -> 420f;
            case LEAVES -> 260f;
            default -> 185f;
        };
        tone(pitch, 0.085f, Wave.NOISE_TONE, 0.55f);
    }

    public void place() { tone(145f, 0.07f, Wave.SQUARE, 0.45f); }
    public void hit() { tone(85f, 0.12f, Wave.NOISE_TONE, 0.7f); }
    public void hurt() { sequence(new float[]{155f, 105f}, 0.09f, Wave.SAW, 0.62f); }
    public void craft() { sequence(new float[]{392f, 523.25f, 659.25f}, 0.085f, Wave.SINE, 0.55f); }
    public void treasure() { sequence(new float[]{261.63f, 329.63f, 392f, 523.25f}, 0.12f, Wave.SINE, 0.7f); }
    public void companion() { sequence(new float[]{392f, 493.88f, 587.33f, 783.99f, 659.25f}, 0.14f, Wave.SINE, 0.74f); }
    public void thunder() { tone(52f, 0.75f, Wave.NOISE_TONE, 0.85f); }

    private enum Wave { SINE, SQUARE, SAW, NOISE_TONE }

    private void tone(float frequency, float duration, Wave wave, float gain) {
        if (!enabled) return;
        audioThread.submit(() -> play(new float[]{frequency}, duration, wave, gain));
    }

    private void sequence(float[] frequencies, float noteDuration, Wave wave, float gain) {
        if (!enabled) return;
        audioThread.submit(() -> play(frequencies, noteDuration, wave, gain));
    }

    private void play(float[] frequencies, float noteDuration, Wave wave, float gain) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, 4096);
            line.start();
            int noise = 0x1234567;
            for (float frequency : frequencies) {
                int samples = Math.max(1, (int) (SAMPLE_RATE * noteDuration));
                byte[] data = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    double phase = i * frequency * Math.PI * 2 / SAMPLE_RATE;
                    double signal = switch (wave) {
                        case SINE -> Math.sin(phase);
                        case SQUARE -> Math.sin(phase) >= 0 ? 0.72 : -0.72;
                        case SAW -> 2.0 * ((i * frequency / SAMPLE_RATE) % 1.0) - 1.0;
                        case NOISE_TONE -> {
                            noise ^= noise << 13;
                            noise ^= noise >>> 17;
                            noise ^= noise << 5;
                            yield Math.sin(phase) * 0.42 + ((noise & 0xffff) / 32768.0 - 1.0) * 0.58;
                        }
                    };
                    double attack = Math.min(1.0, i / (SAMPLE_RATE * 0.008));
                    double decay = Math.pow(1.0 - i / (double) samples, 1.7);
                    short value = (short) (signal * attack * decay * gain * volume * Short.MAX_VALUE);
                    data[i * 2] = (byte) (value & 0xff);
                    data[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
                }
                line.write(data, 0, data.length);
            }
            line.drain();
            line.close();
        } catch (Exception ignored) {
            enabled = false;
        }
    }

    public boolean enabled() { return enabled; }
    public void toggle() { enabled = !enabled; }
    public void setVolume(float value) { volume = Math.max(0f, Math.min(1f, value)); }

    @Override
    public void close() { audioThread.shutdownNow(); }
}
