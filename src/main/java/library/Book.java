package library;

import java.time.LocalDate;

/**
 * A book, identified by its ISBN and written by an author.
 */
public class Book extends Medium {
	private static final long serialVersionUID = 1L;

	private String author;

	/**
	 * Creates a new book.
	 *
	 * @param title the title of the book
	 * @param date the publication date
	 * @param publisher the publisher
	 * @param language the language the book is written in
	 * @param author the author of the book
	 * @param isbn the ISBN identifying the book
	 */
	public Book(String title, LocalDate date, String publisher, String language, String author, long isbn) {
		super(title, date, publisher, language, isbn);
		this.author = author;
	}

	/**
	 * @return the author of the book
	 */
	public String getAuthor() {
		return author;
	}

	@Override
	public String getTypeLabel() {
		return "Book";
	}

	@Override
	public String getAuthorOrNumber() {
		return author;
	}
}
