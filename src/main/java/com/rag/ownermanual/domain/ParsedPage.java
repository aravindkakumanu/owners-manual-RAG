package com.rag.ownermanual.domain;

/**
 * Parsed view of a manual page (or contiguous page range) produced by the document parser.
 * @param pageNumber 1-based page number in the source PDF, or null if unknown.
 *                   For multi-page segments this is typically the starting page.
 * @param text       Raw text content extracted for this page/segment; must be non-blank.
 * @param section    Optional logical section/heading name if the parser can infer it; null when not available.
 */
public record ParsedPage(
        Integer pageNumber,
        String text,
        String section
) {

    public ParsedPage {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must be non-blank");
        }
    }
}
