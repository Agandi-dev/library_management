package library;

import java.time.LocalDate;

/**
 * A magazine issue, identified by its ISSN and issue number.
 */
public class Magazine extends Medium {
	private static final long serialVersionUID = 1L;

	private int number;

	/**
	 * Creates a new magazine issue.
	 *
	 * @param title the title of the magazine
	 * @param date the publication date
	 * @param publisher the publisher
	 * @param language the language the magazine is written in
	 * @param number the issue number
	 * @param issn the ISSN identifying the magazine
	 */
	public Magazine(String title, LocalDate date, String publisher, String language, int number, long issn) {
		super(title, date, publisher, language, issn);
		this.number = number;
	}

	/**
	 * @return the issue number
	 */
	public int getNumber() {
		return number;
	}

	@Override
	public String getTypeLabel() {
		return "Magazine";
	}

	@Override
	public String getAuthorOrNumber() {
		return String.valueOf(number);
	}
}
