package library;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Records a single borrowing of a {@link Medium}: when it was borrowed, when it is due,
 * and (once returned) when it was actually returned.
 */
public class Borrow implements Serializable {
	private static final long serialVersionUID = 1L;

	private LocalDate dateBorrow;
	private LocalDate dateDue;
	private LocalDate dateReturned;
	private boolean status;

	private Medium mediumBorrowed;

	/**
	 * Creates a new borrow record with an active status.
	 *
	 * @param medium the medium being borrowed
	 * @param dateBorrow the date the medium was borrowed
	 * @param dateDue the date the medium is due back
	 */
	public Borrow(Medium medium, LocalDate dateBorrow, LocalDate dateDue) {
		this.dateBorrow = dateBorrow;
		this.dateDue = dateDue;
		mediumBorrowed = medium;
		status = true;
	}

	/**
	 * @return the ISBN/ISSN of the borrowed medium
	 */
	public long getIdNumber() {
		return mediumBorrowed.getIdNumber();
	}

	/**
	 * @return the title of the borrowed medium
	 */
	public String getTitle() {
		return mediumBorrowed.getTitle();
	}

	/**
	 * @return the date the medium was borrowed
	 */
	public LocalDate getDateBorrow() {
		return dateBorrow;
	}

	/**
	 * @return the date the medium was returned, or {@code null} if it is still borrowed
	 */
	public LocalDate getDateReturned() {
		return dateReturned;
	}

	/**
	 * @param dateReturned the date the medium was returned
	 */
	public void setDateReturned(LocalDate dateReturned) {
		this.dateReturned = dateReturned;
	}

	/**
	 * @return {@code true} if the medium is still borrowed, {@code false} if it has been returned
	 */
	public boolean getStatus() {
		return status;
	}

	/**
	 * @param status {@code true} if the medium is still borrowed, {@code false} if it has been returned
	 */
	public void setStatus(boolean status) {
		this.status = status;
	}

	/**
	 * @return the date the medium is due back
	 */
	public LocalDate getDateDue() {
		return dateDue;
	}
}
