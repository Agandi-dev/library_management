# Library Management

A desktop application for managing a small library, built with Java and Swing. Books and magazines can be catalogued, borrowed and returned, and the data is saved automatically between sessions.

## Features

- **Media management** – add, delete and search books and magazines (title, date, publisher, language, author/issue number, ISBN/ISSN). Search matches the exact title.
- **Borrowing and returning** – borrow a medium for a number of days and return it later. Clicking a table row fills in the ISBN/ISSN automatically.
- **Reports** – borrowing history and a list of overdue borrowings.
- **Date picker** – a small calendar popup (the `...` button) next to every date field, with today highlighted.
- **Input validation** – missing fields, invalid dates and numbers, duplicate ISBN/ISSN, unknown or already-borrowed media, and return dates before the borrow date are rejected with an error message.
- **CSV import/export** – export the catalog to a CSV file and import it again.
- **Persistence** – the catalog and the full borrowing history are saved when the window is closed and loaded again on the next start.

## Requirements

- Java 8 or newer (JDK)
- Optional: Eclipse, the project is set up as an Eclipse/Maven project

## Getting started

### Eclipse

1. Import the project (*File → Import → Existing Maven Projects*, or *Existing Projects into Workspace*).
2. Right-click `LibraryManagementApp.java` and choose *Run As → Java Application*.

### Command line

From the project root:

```bash
javac -d target/classes src/main/java/library/*.java
java -cp target/classes library.LibraryManagementApp
```

## Usage

| Tab | What it does |
| --- | --- |
| **Media** | Add a book or magazine with the form on the left, delete the selected row, search by title, and import/export CSV files. |
| **Borrow** | Enter an ISBN/ISSN (or click a row in the table of available media), a borrow date and a duration in days. |
| **Return** | Enter an ISBN/ISSN (or click a row in the table of borrowed media) and a return date. |
| **Reports** | Show the complete borrowing history or only the overdue borrowings. |

### CSV format

Exported files start with a header row followed by one medium per line:

```csv
type,title,date,publisher,language,author_or_number,isbn_issn
Book,The Hobbit,1937-09-21,Houghton Mifflin,English,J.R.R. Tolkien,9780345339683
Magazine,National Geographic,2024-03-01,NG Society,English,245,10346178
```

- `type` is `Book` or `Magazine`.
- `date` uses the format `yyyy-mm-dd`.
- `author_or_number` is the author for a book and the issue number for a magazine.
- Fields containing commas or quotes are wrapped in double quotes. Rows that are malformed or whose ISBN/ISSN already exists are skipped on import.

### Saved data

When the window is closed, the application writes `library_data.dat` to the directory it was started from (the project root when run from Eclipse). The file is read again on the next start. On the very first start the library is empty. The file is listed in `.gitignore`.

## Project structure

```
src/main/java/library/
├── Medium.java                  abstract base class for all media
├── Book.java                    a medium with an author (ISBN)
├── Magazine.java                a medium with an issue number (ISSN)
├── Borrow.java                  one borrowing of a medium (dates, status)
├── Library.java                 catalog and borrow history, CSV import/export, save/load
├── ImportResult.java            result of a CSV import (added/skipped rows)
├── LibraryManagementFrame.java  Swing user interface
└── LibraryManagementApp.java    application entry point
```

## License

This project is licensed under the GNU Lesser General Public License v2.1. See [LICENSE](LICENSE) for the full text.
