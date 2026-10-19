package library;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Base class for a medium that can be stored in a {@link Library} and borrowed.
 * A medium is identified by its ISBN (for a {@link Book}) or ISSN (for a {@link Magazine}).
 */
public abstract class Medium implements Serializable {
	private static final long serialVersionUID = 1L;

	private String title;
	private LocalDate date;
	private String publisher;
	private String language;
	private long idNumber; //ISBN or ISSN

	/**
	 * Creates a new medium.
	 *
	 * @param title the title of the medium
	 * @param date the publication date
	 * @param publisher the publisher
	 * @param language the language the medium is written in
	 * @param idNumber the ISBN or ISSN identifying the medium
	 */
	public Medium(String title, LocalDate date, String publisher, String language, long idNumber) {
		this.title = title;
		this.date = date;
		this.publisher = publisher;
		this.language = language;
		this.idNumber = idNumber;
	}

	/**
	 * @return the ISBN (for a book) or ISSN (for a magazine)
	 */
	public long getIdNumber() {
		return idNumber;
	}

	/**
	 * @return the title of the medium
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the publication date
	 */
	public LocalDate getDate() {
		return date;
	}

	/**
	 * @return the publisher
	 */
	public String getPublisher() {
		return publisher;
	}

	/**
	 * @return the language the medium is written in
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @return a short label identifying the concrete kind of medium (e.g. "Book" or
	 *         "Magazine"), overridden polymorphically by each subclass
	 */
	public abstract String getTypeLabel();

	/**
	 * @return the type-specific detail for this medium: a book's author, or a
	 *         magazine's issue number as text, overridden polymorphically by each
	 *         subclass
	 */
	public abstract String getAuthorOrNumber();
}
