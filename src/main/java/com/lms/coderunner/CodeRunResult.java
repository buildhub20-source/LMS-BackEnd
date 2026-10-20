package com.lms.coderunner;

/**
 * Outcome of running one input.
 *
 * @param status ACCEPTED (ran to completion), COMPILE_ERROR, RUNTIME_ERROR,
 *               TIME_LIMIT_EXCEEDED or SANDBOX_ERROR. Comparing stdout with the
 *               expected output is the caller's job.
 */
public record CodeRunResult(String status,
                            String stdout,
                            String stderr,
                            String compileOutput,
                            Integer timeMs,
                            Integer memoryKb) {}
