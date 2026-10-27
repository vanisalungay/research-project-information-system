package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry of the Terminal Report's "Literature Cited" section.
 *
 * <p>The fields mirror the bibliographic information the official format lists. Only what
 * the proponent entered is stored: the system never fills in or guesses missing
 * bibliographic data.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerminalReportReference {

    /** Author(s) - the entry is listed alphabetically by this value. */
    private String author;

    private String year;
    private String title;

    /** Edition, for a book source. */
    private String edition;

    /** Place of publication, for a book source. */
    private String place;

    /** Journal or publication name, for an article source. */
    private String journal;

    /** Page number(s), when applicable. */
    private String pages;
}
