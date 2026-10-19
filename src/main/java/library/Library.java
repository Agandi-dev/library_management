package library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a catalog of {@link Medium} items and the history of {@link Borrow} records
 * for them, and supports importing/exporting the catalog as CSV or saving/loading the
 * full state (catalog and borrow history) to/from a file.
 */
public class Library {

	private static final String CSV_HEADER = "type,title,date,publisher,language,author_or_number,isbn_issn";

	private ArrayList <Medium> media;
	private ArrayList <Borrow> borrows;

	/**
	 * Creates an empty library.
	 */
	public Library() {
		this.media = new ArrayList<>();
		this.borrows = new ArrayList<>();
	}

	/**
	 * Adds a new book to the catalog, unless a medium with the same ISBN already exists.
	 *
	 * @param name the title of the book
	 * @param date the publication date
	 * @param publisher the publisher
	 * @param language the language the book is written in
	 * @param author the author of the book
	 * @param isbn the ISBN identifying the book
	 * @return {@code true} if the book was added, {@code false} if an ISBN/ISSN collision occurred
	 */
	public boolean addMedium(String name, LocalDate date, String publisher, String language, String author, long isbn) {
		for(Medium m: media) {
			if(m.getIdNumber() == isbn) {
				return false; // there is already a book with this isbn
			}
		}
		media.add(new Book(name, date, publisher, language, author, isbn));
		return true;
	}

	/**
	 * Adds a new magazine to the catalog, unless a medium with the same ISSN already exists.
	 *
	 * @param name the title of the magazine
	 * @param date the publication date
	 * @param publisher the publisher
	 * @param language the language the magazine is written in
	 * @param number the issue number
	 * @param issn the ISSN identifying the magazine
	 * @return {@code true} if the magazine was added, {@code false} if an ISBN/ISSN collision occurred
	 */
	public boolean addMedium(String name, LocalDate date, String publisher, String language, int number, long issn) {
		for(Medium m: media) {
			if(m.getIdNumber() == issn) {
				return false; // there is already a medium with this issn
			}
		}
		media.add(new Magazine(name, date, publisher, language, number, issn));
		return true;
	}

	/**
	 * Removes the medium at the given catalog index.
	 *
	 * @param n the index of the medium to remove
	 * @return {@code true} if a medium was removed, {@code false} if the index was out of range
	 */
	public boolean deleteMedium(int n) {
		if(n >= 0 && n < media.size()) {
			media.remove(n);
			return true;
		}
		return false;
	}

	/**
	 * @return the number of media currently in the catalog
	 */
	public int getMediaCount() {
		return media.size();
	}

	/**
	 * @param n the catalog index
	 * @return the medium at that index, or {@code null} if the index is out of range
	 */
	public Medium getMedium(int n) {
		if(n >= 0 && n < media.size()) {
			return media.get(n);
		}
		return null;
	}

	/**
	 * Saves the entire catalog and borrow history to a file using Java serialization,
	 * so it can be restored later with {@link #load(File)}.
	 *
	 * @param file the file to write to
	 * @throws IOException if the file cannot be written
	 */
	public void save(File file) throws IOException {
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
			out.writeObject(media);
			out.writeObject(borrows);
		}
	}

	/**
	 * Restores the catalog and borrow history previously written by {@link #save(File)},
	 * replacing whatever is currently in this library.
	 *
	 * @param file the file to read from
	 * @throws IOException if the file cannot be read
	 * @throws ClassNotFoundException if the file's content doesn't match the expected classes
	 */
	@SuppressWarnings("unchecked")
	public void load(File file) throws IOException, ClassNotFoundException {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
			media = (ArrayList<Medium>) in.readObject();
			borrows = (ArrayList<Borrow>) in.readObject();
		}
	}

	/**
	 * Writes the entire catalog to a CSV file, one medium per line, with a header row.
	 *
	 * @param file the file to write to
	 * @throws IOException if the file cannot be written
	 */
	public void exportMedia(File file) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(CSV_HEADER);
			writer.newLine();
			for(Medium m: media) {
				writer.write(String.join(",",
						escapeCsv(m.getTypeLabel()),
						escapeCsv(m.getTitle()),
						escapeCsv(m.getDate().toString()),
						escapeCsv(m.getPublisher()),
						escapeCsv(m.getLanguage()),
						escapeCsv(m.getAuthorOrNumber()),
						escapeCsv(String.valueOf(m.getIdNumber()))));
				writer.newLine();
			}
		}
	}

	/**
	 * Reads a CSV file previously written by {@link #exportMedia(File)} and adds its rows
	 * to the catalog. Malformed rows and ISBN/ISSN collisions with existing media are
	 * skipped rather than aborting the whole import.
	 *
	 * @param file the CSV file to read
	 * @return the number of media added and skipped
	 * @throws IOException if the file cannot be read
	 */
	public ImportResult importMedia(File file) throws IOException {
		int added = 0;
		int skipped = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line = reader.readLine(); // header
			while((line = reader.readLine()) != null) {
				if(line.trim().isEmpty()) {
					continue;
				}
				List<String> fields = parseCsvLine(line);
				if(fields.size() != 7) {
					skipped++;
					continue;
				}
				try {
					String type = fields.get(0).trim();
					String title = fields.get(1);
					LocalDate date = LocalDate.parse(fields.get(2).trim());
					String publisher = fields.get(3);
					String language = fields.get(4);
					String authorOrNumber = fields.get(5);
					long idNumber = Long.parseLong(fields.get(6).trim());

					boolean wasAdded;
					if("Book".equalsIgnoreCase(type)) {
						wasAdded = addMedium(title, date, publisher, language, authorOrNumber, idNumber);
					} else if("Magazine".equalsIgnoreCase(type)) {
						int number = Integer.parseInt(authorOrNumber.trim());
						wasAdded = addMedium(title, date, publisher, language, number, idNumber);
					} else {
						skipped++;
						continue;
					}
					if(wasAdded) {
						added++;
					} else {
						skipped++;
					}
				} catch(RuntimeException ex) {
					skipped++;
				}
			}
		}
		return new ImportResult(added, skipped);
	}

	/**
	 * Escapes a single CSV field, quoting it if it contains a comma, quote, or newline.
	 */
	private static String escapeCsv(String value) {
		if(value == null) {
			return "";
		}
		String escaped = value.replace("\"", "\"\"");
		if(value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
			return "\"" + escaped + "\"";
		}
		return escaped;
	}

	/**
	 * Splits one CSV line into its fields, honoring double-quoted fields with embedded
	 * commas and escaped ({@code ""}) quotes.
	 */
	private static List<String> parseCsvLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(inQuotes) {
				if(c == '"') {
					if(i + 1 < line.length() && line.charAt(i + 1) == '"') {
						current.append('"');
						i++;
					} else {
						inQuotes = false;
					}
				} else {
					current.append(c);
				}
			} else {
				if(c == '"') {
					inQuotes = true;
				} else if(c == ',') {
					fields.add(current.toString());
					current.setLength(0);
				} else {
					current.append(c);
				}
			}
		}
		fields.add(current.toString());
		return fields;
	}

	/**
	 * Finds the first medium with the given title.
	 *
	 * @param search the title to search for
	 * @return the matching medium, or {@code null} if none was found
	 */
	public Medium searchMedium(String search) {
		for(Medium m: media) {
			if(m.getTitle().equals(search)) {
				return m;
			}
		}
		return null;
	}

	/**
	 * Borrows the medium with the given ISBN/ISSN, if it exists and is not already
	 * borrowed.
	 *
	 * @param idNumber the ISBN/ISSN of the medium to borrow
	 * @param dateBorrow the date the medium is borrowed
	 * @param duration the number of days until the medium is due
	 * @return {@code true} if the medium was borrowed, {@code false} if it doesn't exist
	 *         or is already borrowed
	 */
	public boolean borrow(long idNumber, LocalDate dateBorrow, long duration) {
		for(Medium m: media) {
			if(m.getIdNumber() == idNumber) {
				for(Borrow b: borrows) {
					if(b.getIdNumber() == idNumber && b.getStatus()) {
						return false; //already borrowed
					}
				}
				borrows.add(new Borrow(m, dateBorrow, dateBorrow.plusDays(duration)));
				return true; //found the media
			}
		}
		return false; //no media with this ISBN/ISSN
	}

	/**
	 * @return the number of borrow records (active and returned)
	 */
	public int getBorrowCount() {
		return borrows.size();
	}

	/**
	 * @param n the index into the borrow history
	 * @return the borrow record at that index, or {@code null} if the index is out of range
	 */
	public Borrow getBorrow(int n) {
		if(n >= 0 && n < borrows.size()) {
			return borrows.get(n);
		}
		return null;
	}

	/**
	 * Marks the active borrow for the given ISBN/ISSN as returned.
	 *
	 * @param idNumber the ISBN/ISSN of the medium being returned
	 * @param dateReturned the date the medium was returned
	 * @return {@code true} if an active borrow was found and marked as returned,
	 *         {@code false} otherwise
	 */
	public boolean refund(long idNumber, LocalDate dateReturned) {
		for(Borrow b: borrows) {
			if(b.getIdNumber() == idNumber && b.getStatus()) {
				b.setStatus(false);
				b.setDateReturned(dateReturned);
				return true;
			}
		}
		return false;
	}
}
