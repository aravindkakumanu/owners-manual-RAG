package com.rag.ownermanual.exception;

/**
 * <p>Signals that the LLM call (Spring AI ChatClient / ChatModel) failed while
 * generating an answer – for example due to timeouts, rate limits, or upstream
 * provider errors.</p>
 * <p> A service-layer exception used to represent "answer generation
 * failed due to downstream LLM issues". The global exception handler maps this
 * to a 5xx HTTP response with a safe, generic message.</p>
 */
public class DownstreamLlmException extends RuntimeException {

    public DownstreamLlmException(String message, Throwable cause) {
        super(message, cause);
    }
}

