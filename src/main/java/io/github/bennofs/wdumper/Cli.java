package io.github.bennofs.wdumper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bennofs.wdumper.ext.ZstdDumpFile;
import io.github.bennofs.wdumper.interfaces.DumpStatusHandler;
import io.github.bennofs.wdumper.interfaces.RunnerStatusHandler;
import io.github.bennofs.wdumper.spec.DumpSpec;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Provides a simple CLI interface to test the dump generation and generate single dumps from a spec.
 */
public class Cli implements Runnable {
    @CommandLine.Parameters(paramLabel = "DUMP", arity = "1", index = "0", description = "JSON dump from wikidata to process")
    private Path dumpFilePath;

    @CommandLine.Parameters(paramLabel = "SPEC", arity = "1", index = "1", description = "Path to the JSON spec for the dump")
    private Path specFilePath;

    @Override
    public void run() {
        final DumpRunner runner = DumpRunner.create(1, new ZstdDumpFile(dumpFilePath.toString()), FileSystems.getDefault().getPath("."));

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final DumpSpec spec = mapper.readValue(this.specFilePath.toFile(), DumpSpec.class);

            runner.addDumpTask(1, spec, new DumpStatusHandler() {
                @Override
                public void reportError(ErrorLevel level, String message) {
                    System.err.println("[" + level.toString() + "] " + message);
                }
            });
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        final long start = System.currentTimeMillis();
        runner.run(new RunnerStatusHandler() {
            @Override
            public void start() {

            }

            @Override
            public void reportProgress(int count) {
                System.err.println("processed " + count + " items");
            }

            @Override
            public void done() {
                final long end = System.currentTimeMillis();
                System.out.println("time: " + (end - start) / 1000);
            }
        });
    }

    public static void main(String[] args) {
        new CommandLine(new Cli()).execute(args);
    }
}
