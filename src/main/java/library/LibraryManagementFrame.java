package library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 * Main application window: a tabbed Swing UI for managing the {@link Library} catalog
 * (add/delete/search/import/export media), borrowing and returning media, and viewing
 * borrowing reports.
 */
@SuppressWarnings("serial")
public class LibraryManagementFrame extends JFrame {

	private static final File SAVE_FILE = new File("library_data.dat");

	private final Library library = new Library();

	private int formPanelWidth;
	private JTable mediaTable;
	private DefaultTableModel mediaTableModel;
	private DefaultTableModel borrowsTableModel;
	private DefaultTableModel availableMediaTableModel;
	private DefaultTableModel reportsTableModel;
	private JLabel statusLabel;

	/**
	 * Loads the library catalog from disk if a save file exists, and assembles and
	 * shows the main window. The catalog is saved back to disk when the window is
	 * closed.
	 */
	public LibraryManagementFrame() {
		loadSavedData();
		initAvailableMediaTableModel();
		initBorrowsTableModel();

		setTitle("Library Management");
		setSize(900, 650);
		setMinimumSize(new Dimension(750, 500));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				saveData();
			}
		});

		setLayout(new BorderLayout());
		add(createHeader(), BorderLayout.NORTH);
		add(createTabs(), BorderLayout.CENTER);
		add(createStatusBar(), BorderLayout.SOUTH);

		setVisible(true);
	}

	/**
	 * Loads the catalog and borrow history from {@link #SAVE_FILE} if it exists,
	 * leaving the library empty if there's nothing saved yet or loading fails.
	 */
	private void loadSavedData() {
		if (!SAVE_FILE.exists()) {
			return;
		}
		try {
			library.load(SAVE_FILE);
		} catch (IOException | ClassNotFoundException ex) {
			showError("Could not load saved data (" + ex.getMessage() + "), starting with an empty library instead.");
		}
	}

	/**
	 * Saves the catalog and borrow history to {@link #SAVE_FILE}.
	 */
	private void saveData() {
		try {
			library.save(SAVE_FILE);
		} catch (IOException ex) {
			showError("Could not save data: " + ex.getMessage());
		}
	}

	/**
	 * Creates the table model backing the "currently borrowed" table shown on the
	 * Return tab, and fills it with the current data.
	 */
	private void initBorrowsTableModel() {
		String[] columns = { "Title", "ISBN / ISSN", "Date borrowed", "Date due" };
		borrowsTableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		refreshBorrowsTable();
	}

	/**
	 * Rebuilds the "currently borrowed" table from the library's active borrow records,
	 * then refreshes the available-media table since borrow status affects it too.
	 */
	private void refreshBorrowsTable() {
		borrowsTableModel.setRowCount(0);
		for (int i = 0; i < library.getBorrowCount(); i++) {
			Borrow b = library.getBorrow(i);
			if (b.getStatus()) {
				borrowsTableModel.addRow(
						new Object[] { b.getTitle(), b.getIdNumber(), b.getDateBorrow(), b.getDateDue() });
			}
		}
		refreshAvailableMediaTable();
	}

	/**
	 * Creates the table model backing the "currently available" table shown on the
	 * Borrow tab, and fills it with the current data.
	 */
	private void initAvailableMediaTableModel() {
		String[] columns = { "Title", "Type", "Author / Number", "ISBN / ISSN", "Publisher", "Language", "Date" };
		availableMediaTableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		refreshAvailableMediaTable();
	}

	/**
	 * Rebuilds the "currently available" table with every medium that has no active
	 * borrow.
	 */
	private void refreshAvailableMediaTable() {
		availableMediaTableModel.setRowCount(0);
		for (int i = 0; i < library.getMediaCount(); i++) {
			Medium medium = library.getMedium(i);
			if (isAvailable(medium.getIdNumber())) {
				availableMediaTableModel.addRow(mediumToRow(medium));
			}
		}
	}

	/**
	 * @param idNumber the ISBN/ISSN to check
	 * @return {@code true} if no active borrow exists for that ISBN/ISSN
	 */
	private boolean isAvailable(long idNumber) {
		for (int i = 0; i < library.getBorrowCount(); i++) {
			Borrow b = library.getBorrow(i);
			if (b.getIdNumber() == idNumber && b.getStatus()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Converts a medium into a table row matching the media/available-media table
	 * columns (Title, Type, Author/Number, ISBN/ISSN, Publisher, Language, Date).
	 */
	private Object[] mediumToRow(Medium medium) {
		return new Object[] { medium.getTitle(), medium.getTypeLabel(), medium.getAuthorOrNumber(),
				medium.getIdNumber(), medium.getPublisher(), medium.getLanguage(), medium.getDate() };
	}

	/**
	 * @param idNumber the ISBN/ISSN to look up
	 * @return the matching medium, or {@code null} if none is in the catalog
	 */
	private Medium findMediumById(long idNumber) {
		for (int i = 0; i < library.getMediaCount(); i++) {
			Medium medium = library.getMedium(i);
			if (medium.getIdNumber() == idNumber) {
				return medium;
			}
		}
		return null;
	}

	/**
	 * @param idNumber the ISBN/ISSN to look up
	 * @return the active borrow record for that ISBN/ISSN, or {@code null} if none is
	 *         currently borrowed
	 */
	private Borrow findActiveBorrow(long idNumber) {
		for (int i = 0; i < library.getBorrowCount(); i++) {
			Borrow b = library.getBorrow(i);
			if (b.getIdNumber() == idNumber && b.getStatus()) {
				return b;
			}
		}
		return null;
	}

	/**
	 * @return the bold title label shown at the top of the window
	 */
	private JComponent createHeader() {
		JLabel title = new JLabel("Library Management System", SwingConstants.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
		title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		return title;
	}

	/**
	 * @return the tabbed pane with the Media, Borrow, Return, and Reports tabs
	 */
	private JTabbedPane createTabs() {
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Media", createMediaTab());
		tabs.addTab("Borrow", createBorrowTab());
		tabs.addTab("Return", createReturnTab());
		tabs.addTab("Reports", createReportsTab());
		return tabs;
	}

	/**
	 * Builds the Media tab: a form for adding/deleting/searching/importing/exporting
	 * media, and a table listing the full catalog.
	 */
	private JPanel createMediaTab() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = -1;
		gbc.weightx = 1;

		JComboBox<String> typeBox = new JComboBox<>(new String[] { "Book", "Magazine" });
		JTextField titleField = new JTextField(15);
		JTextField dateField = new JTextField(15);
		JTextField publisherField = new JTextField(15);
		JTextField languageField = new JTextField(15);
		JTextField authorNumberField = new JTextField(15);
		JTextField isbnIssnField = new JTextField(15);

		addField(form, gbc, "Type", typeBox);
		addField(form, gbc, "Title", titleField);
		addField(form, gbc, "Date (yyyy-mm-dd)", createDateField(dateField));
		addField(form, gbc, "Publisher", publisherField);
		addField(form, gbc, "Language", languageField);
		addField(form, gbc, "Author / Number", authorNumberField);
		addField(form, gbc, "ISBN / ISSN", isbnIssnField);

		JButton addButton = new JButton("Add");
		JButton deleteButton = new JButton("Delete Selected");
		JPanel formButtons = new JPanel();
		formButtons.add(addButton);
		formButtons.add(deleteButton);
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 2;
		form.add(formButtons, gbc);

		gbc.gridy++;
		form.add(new JSeparator(), gbc);

		JTextField searchField = new JTextField();
		JButton searchButton = new JButton("Search");
		JPanel searchRow = new JPanel(new BorderLayout(5, 5));
		searchRow.add(searchField, BorderLayout.CENTER);
		searchRow.add(searchButton, BorderLayout.EAST);
		gbc.gridy++;
		form.add(new JLabel("Search by title"), gbc);
		gbc.gridy++;
		form.add(searchRow, gbc);

		gbc.gridy++;
		form.add(new JSeparator(), gbc);

		JButton importButton = new JButton("Import");
		JButton exportButton = new JButton("Export");
		JPanel importExportRow = new JPanel();
		importExportRow.add(importButton);
		importExportRow.add(exportButton);
		gbc.gridy++;
		form.add(importExportRow, gbc);

		formPanelWidth = form.getPreferredSize().width;
		panel.add(form, BorderLayout.WEST);

		String[] columns = { "Title", "Type", "Author / Number", "ISBN / ISSN", "Publisher", "Language", "Date" };
		mediaTableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		mediaTable = new JTable(mediaTableModel);
		mediaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		refreshMediaTable();
		panel.add(new JScrollPane(mediaTable), BorderLayout.CENTER);

		addButton.addActionListener(e -> handleAddMedium(typeBox, titleField, dateField, publisherField,
				languageField, authorNumberField, isbnIssnField));
		deleteButton.addActionListener(e -> handleDeleteSelectedMedium());
		searchButton.addActionListener(e -> handleSearchMedium(searchField.getText()));
		importButton.addActionListener(e -> handleImportMedia());
		exportButton.addActionListener(e -> handleExportMedia());

		return panel;
	}

	/**
	 * Prompts the user for a CSV file and imports it into the catalog via
	 * {@link Library#importMedia(File)}, reporting how many rows were added/skipped.
	 */
	private void handleImportMedia() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		try {
			ImportResult result = library.importMedia(chooser.getSelectedFile());
			refreshMediaTable();
			setStatus("Imported " + result.getAddedCount() + " medium(s), skipped " + result.getSkippedCount() + ".");
		} catch (IOException ex) {
			showError("Could not read file: " + ex.getMessage());
		}
	}

	/**
	 * Prompts the user for a destination file and writes the catalog to it as CSV via
	 * {@link Library#exportMedia(File)}.
	 */
	private void handleExportMedia() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
		chooser.setSelectedFile(new File("media.csv"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = chooser.getSelectedFile();
		if (!file.getName().toLowerCase().endsWith(".csv")) {
			file = new File(file.getParentFile(), file.getName() + ".csv");
		}
		try {
			library.exportMedia(file);
			setStatus("Exported media to " + file.getName() + ".");
		} catch (IOException ex) {
			showError("Could not write file: " + ex.getMessage());
		}
	}

	/**
	 * Validates the Add Medium form and, if valid, adds the medium to the catalog,
	 * clears the form, and refreshes the media table. Shows an error dialog for any
	 * validation failure or ISBN/ISSN collision.
	 */
	private void handleAddMedium(JComboBox<String> typeBox, JTextField titleField, JTextField dateField,
			JTextField publisherField, JTextField languageField, JTextField authorNumberField,
			JTextField isbnIssnField) {
		String title = titleField.getText().trim();
		String dateText = dateField.getText().trim();
		String publisher = publisherField.getText().trim();
		String language = languageField.getText().trim();
		String authorNumber = authorNumberField.getText().trim();
		String idText = isbnIssnField.getText().trim();

		if (title.isEmpty() || dateText.isEmpty() || publisher.isEmpty() || language.isEmpty()
				|| authorNumber.isEmpty() || idText.isEmpty()) {
			showError("Please fill in all fields.");
			return;
		}

		LocalDate date;
		try {
			date = LocalDate.parse(dateText);
		} catch (DateTimeParseException ex) {
			showError("Date must be in the format yyyy-mm-dd.");
			return;
		}

		long idNumber;
		try {
			idNumber = Long.parseLong(idText);
		} catch (NumberFormatException ex) {
			showError("ISBN / ISSN must be a number.");
			return;
		}

		boolean added;
		if ("Book".equals(typeBox.getSelectedItem())) {
			added = library.addMedium(title, date, publisher, language, authorNumber, idNumber);
		} else {
			int number;
			try {
				number = Integer.parseInt(authorNumber);
			} catch (NumberFormatException ex) {
				showError("Number must be numeric for a Magazine.");
				return;
			}
			added = library.addMedium(title, date, publisher, language, number, idNumber);
		}

		if (!added) {
			showError("A medium with this ISBN / ISSN already exists.");
			return;
		}

		refreshMediaTable();
		titleField.setText("");
		dateField.setText("");
		publisherField.setText("");
		languageField.setText("");
		authorNumberField.setText("");
		isbnIssnField.setText("");
		setStatus("Added \"" + title + "\".");
	}

	/**
	 * Deletes the medium currently selected in the media table, or shows an error if
	 * nothing is selected.
	 */
	private void handleDeleteSelectedMedium() {
		int row = mediaTable.getSelectedRow();
		if (row < 0) {
			showError("Select a medium to delete first.");
			return;
		}
		String title = (String) mediaTableModel.getValueAt(row, 0);
		library.deleteMedium(row);
		refreshMediaTable();
		setStatus("Deleted \"" + title + "\".");
	}

	/**
	 * Looks up a medium by title and, if found, selects and scrolls to its row in the
	 * media table; otherwise reports "not found" in the status bar.
	 *
	 * @param search the title to search for
	 */
	private void handleSearchMedium(String search) {
		String title = search.trim();
		if (title.isEmpty()) {
			showError("Enter a title to search for.");
			return;
		}
		Medium found = library.searchMedium(title);
		if (found == null) {
			mediaTable.clearSelection();
			setStatus("No medium found with title \"" + title + "\".");
			return;
		}
		for (int row = 0; row < mediaTableModel.getRowCount(); row++) {
			if (title.equals(mediaTableModel.getValueAt(row, 0))) {
				mediaTable.setRowSelectionInterval(row, row);
				mediaTable.scrollRectToVisible(mediaTable.getCellRect(row, 0, true));
				break;
			}
		}
		setStatus("Found \"" + title + "\".");
	}

	/**
	 * Rebuilds the media table from the current catalog, then refreshes the
	 * available-media table since it derives from the same data.
	 */
	private void refreshMediaTable() {
		mediaTableModel.setRowCount(0);
		for (int i = 0; i < library.getMediaCount(); i++) {
			mediaTableModel.addRow(mediumToRow(library.getMedium(i)));
		}
		refreshAvailableMediaTable();
	}

	/**
	 * Shows a modal error dialog with the given message.
	 */
	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Updates the status bar text at the bottom of the window.
	 */
	private void setStatus(String message) {
		if (statusLabel != null) {
			statusLabel.setText(message);
		}
	}

	/**
	 * Builds the Borrow tab: a form for borrowing a medium by ISBN/ISSN, and a table
	 * of media currently available to borrow.
	 */
	private JPanel createBorrowTab() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = -1;
		gbc.weightx = 1;

		JTextField idField = new JTextField(15);
		JTextField dateField = new JTextField(15);
		JTextField durationField = new JTextField(15);
		JButton borrowButton = new JButton("Borrow");

		addFieldStacked(form, gbc, "ISBN / ISSN", idField);
		addFieldStacked(form, gbc, "Borrow date (yyyy-mm-dd)", createDateField(dateField));
		addFieldStacked(form, gbc, "Duration (days)", durationField);

		gbc.gridy++;
		form.add(borrowButton, gbc);

		form.setPreferredSize(new Dimension(formPanelWidth, form.getPreferredSize().height));
		panel.add(form, BorderLayout.WEST);
		panel.add(createAvailableMediaTable("Currently available", idField), BorderLayout.CENTER);

		borrowButton.addActionListener(e -> handleBorrow(idField, dateField, durationField));

		return panel;
	}

	/**
	 * Validates the Borrow form and, if valid, borrows the medium, clears the form,
	 * and refreshes the borrow-related tables. Shows an error dialog for any
	 * validation failure, an unknown ISBN/ISSN, or an already-borrowed medium.
	 */
	private void handleBorrow(JTextField idField, JTextField dateField, JTextField durationField) {
		String idText = idField.getText().trim();
		String dateText = dateField.getText().trim();
		String durationText = durationField.getText().trim();

		if (idText.isEmpty() || dateText.isEmpty() || durationText.isEmpty()) {
			showError("Please fill in all fields.");
			return;
		}

		long idNumber;
		try {
			idNumber = Long.parseLong(idText);
		} catch (NumberFormatException ex) {
			showError("ISBN / ISSN must be a number.");
			return;
		}

		LocalDate date;
		try {
			date = LocalDate.parse(dateText);
		} catch (DateTimeParseException ex) {
			showError("Date must be in the format yyyy-mm-dd.");
			return;
		}

		long duration;
		try {
			duration = Long.parseLong(durationText);
		} catch (NumberFormatException ex) {
			showError("Duration must be a number of days.");
			return;
		}

		if (findMediumById(idNumber) == null) {
			showError("No medium found with ISBN / ISSN " + idNumber + ".");
			return;
		}

		boolean borrowed = library.borrow(idNumber, date, duration);
		if (!borrowed) {
			showError("This medium is already borrowed.");
			return;
		}

		refreshBorrowsTable();
		idField.setText("");
		dateField.setText("");
		durationField.setText("");
		setStatus("Borrowed medium with ISBN / ISSN " + idNumber + ".");
	}

	/**
	 * Builds the Return tab: a form for returning a medium by ISBN/ISSN, and a table
	 * of media currently borrowed.
	 */
	private JPanel createReturnTab() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = -1;
		gbc.weightx = 1;

		JTextField idField = new JTextField(15);
		JTextField dateField = new JTextField(15);
		JButton returnButton = new JButton("Return");

		addFieldStacked(form, gbc, "ISBN / ISSN", idField);
		addFieldStacked(form, gbc, "Return date (yyyy-mm-dd)", createDateField(dateField));

		gbc.gridy++;
		form.add(returnButton, gbc);

		form.setPreferredSize(new Dimension(formPanelWidth, form.getPreferredSize().height));
		panel.add(form, BorderLayout.WEST);
		panel.add(createBorrowsTable("Currently borrowed", idField), BorderLayout.CENTER);

		returnButton.addActionListener(e -> handleReturn(idField, dateField));

		return panel;
	}

	/**
	 * Validates the Return form and, if valid, marks the medium as returned, clears
	 * the form, and refreshes the borrow-related tables. Shows an error dialog for
	 * any validation failure, if there is no active borrow for the ISBN/ISSN, or if
	 * the return date is before the date the medium was borrowed.
	 */
	private void handleReturn(JTextField idField, JTextField dateField) {
		String idText = idField.getText().trim();
		String dateText = dateField.getText().trim();

		if (idText.isEmpty() || dateText.isEmpty()) {
			showError("Please fill in all fields.");
			return;
		}

		long idNumber;
		try {
			idNumber = Long.parseLong(idText);
		} catch (NumberFormatException ex) {
			showError("ISBN / ISSN must be a number.");
			return;
		}

		LocalDate date;
		try {
			date = LocalDate.parse(dateText);
		} catch (DateTimeParseException ex) {
			showError("Date must be in the format yyyy-mm-dd.");
			return;
		}

		Borrow activeBorrow = findActiveBorrow(idNumber);
		if (activeBorrow == null) {
			showError("No active borrow found for ISBN / ISSN " + idNumber + ".");
			return;
		}

		if (date.isBefore(activeBorrow.getDateBorrow())) {
			showError("Return date cannot be before the borrow date (" + activeBorrow.getDateBorrow() + ").");
			return;
		}

		library.refund(idNumber, date);
		refreshBorrowsTable();
		idField.setText("");
		dateField.setText("");
		setStatus("Returned medium with ISBN / ISSN " + idNumber + ".");
	}

	/**
	 * @param label the heading shown above the table
	 * @param idField filled with a row's ISBN/ISSN when that row is clicked
	 * @return a panel with a new {@link JTable} view onto the shared borrows table model
	 */
	private JPanel createBorrowsTable(String label, JTextField idField) {
		JTable table = new JTable(borrowsTableModel);
		wireIdFieldOnSelection(table, idField, 1);
		JPanel listPanel = new JPanel(new BorderLayout(5, 5));
		listPanel.add(new JLabel(label), BorderLayout.NORTH);
		listPanel.add(new JScrollPane(table), BorderLayout.CENTER);
		return listPanel;
	}

	/**
	 * @param label the heading shown above the table
	 * @param idField filled with a row's ISBN/ISSN when that row is clicked
	 * @return a panel with a new {@link JTable} view onto the shared available-media
	 *         table model
	 */
	private JPanel createAvailableMediaTable(String label, JTextField idField) {
		JTable table = new JTable(availableMediaTableModel);
		wireIdFieldOnSelection(table, idField, 3);
		JPanel listPanel = new JPanel(new BorderLayout(5, 5));
		listPanel.add(new JLabel(label), BorderLayout.NORTH);
		listPanel.add(new JScrollPane(table), BorderLayout.CENTER);
		return listPanel;
	}

	/**
	 * Fills {@code idField} with the ISBN/ISSN from the clicked row's given column,
	 * so the Borrow/Return forms can be populated by clicking a table entry.
	 */
	private void wireIdFieldOnSelection(JTable table, JTextField idField, int isbnColumn) {
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
				Object idValue = table.getValueAt(table.getSelectedRow(), isbnColumn);
				idField.setText(String.valueOf(idValue));
			}
		});
	}

	/**
	 * Builds the Reports tab: buttons to show the full borrowing history or just the
	 * overdue items, and a table displaying the selected report.
	 */
	private JPanel createReportsTab() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JButton historyButton = new JButton("Show Borrowing History");
		JButton overdueButton = new JButton("Show Overdue");
		JPanel buttons = new JPanel();
		buttons.add(historyButton);
		buttons.add(overdueButton);
		panel.add(buttons, BorderLayout.NORTH);

		JLabel reportLabel = new JLabel("Select a report above");
		String[] columns = { "Title", "ISBN / ISSN", "Date borrowed", "Date due", "Date returned", "Status" };
		reportsTableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		JTable reportsTable = new JTable(reportsTableModel);

		JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
		resultPanel.add(reportLabel, BorderLayout.NORTH);
		resultPanel.add(new JScrollPane(reportsTable), BorderLayout.CENTER);
		panel.add(resultPanel, BorderLayout.CENTER);

		historyButton.addActionListener(e -> {
			fillReportsTable(b -> true);
			reportLabel.setText("Borrowing History");
		});
		overdueButton.addActionListener(e -> {
			fillReportsTable(b -> b.getStatus() && b.getDateDue().isBefore(LocalDate.now()));
			reportLabel.setText("Overdue Items");
		});

		return panel;
	}

	/**
	 * Fills the reports table with every borrow record matching the given filter.
	 *
	 * @param filter selects which borrow records to display
	 */
	private void fillReportsTable(Predicate<Borrow> filter) {
		reportsTableModel.setRowCount(0);
		for (int i = 0; i < library.getBorrowCount(); i++) {
			Borrow b = library.getBorrow(i);
			if (filter.test(b)) {
				Object dateReturned = b.getDateReturned() != null ? b.getDateReturned() : "-";
				reportsTableModel.addRow(new Object[] { b.getTitle(), b.getIdNumber(), b.getDateBorrow(),
						b.getDateDue(), dateReturned, b.getStatus() ? "Borrowed" : "Returned" });
			}
		}
	}

	/**
	 * @return the status bar label shown at the bottom of the window
	 */
	private JComponent createStatusBar() {
		statusLabel = new JLabel("Ready");
		statusLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, statusLabel.getBackground().darker()),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return statusLabel;
	}

	/**
	 * Wraps a date field with a small button that opens a calendar popup for picking
	 * a date instead of typing one.
	 *
	 * @param dateField the field to fill in when a date is picked
	 * @return a panel containing the field and the calendar button
	 */
	private JPanel createDateField(JTextField dateField) {
		JButton pickButton = new JButton("...");
		pickButton.setMargin(new Insets(0, 4, 0, 4));
		pickButton.addActionListener(e -> showDatePicker(dateField, pickButton));

		JPanel wrapper = new JPanel(new BorderLayout(2, 0));
		wrapper.add(dateField, BorderLayout.CENTER);
		wrapper.add(pickButton, BorderLayout.EAST);
		return wrapper;
	}

	/**
	 * Opens a popup calendar below {@code invoker}, starting on the month of the date
	 * currently in {@code dateField} (or the current month if it's empty/invalid).
	 * Picking a day fills {@code dateField} and closes the popup.
	 */
	private void showDatePicker(JTextField dateField, JComponent invoker) {
		LocalDate initial;
		try {
			initial = LocalDate.parse(dateField.getText().trim());
		} catch (RuntimeException ex) {
			initial = LocalDate.now();
		}

		JPopupMenu popup = new JPopupMenu();
		JPanel content = new JPanel(new BorderLayout(4, 4));
		content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		popup.add(content);

		YearMonth[] shownMonth = { YearMonth.from(initial) };
		Runnable[] refresh = new Runnable[1];
		refresh[0] = () -> {
			content.removeAll();
			content.add(createCalendarHeader(shownMonth, refresh), BorderLayout.NORTH);
			content.add(createCalendarGrid(shownMonth[0], dateField, popup), BorderLayout.CENTER);
			content.revalidate();
			content.repaint();
			popup.pack();
		};
		refresh[0].run();

		popup.show(invoker, 0, invoker.getHeight());
	}

	/**
	 * Builds the calendar popup's header: previous/next month buttons around a label
	 * showing the currently displayed month, wired to update {@code shownMonth} and
	 * rerun {@code refresh}.
	 */
	private JPanel createCalendarHeader(YearMonth[] shownMonth, Runnable[] refresh) {
		JButton prevButton = new JButton("<");
		JButton nextButton = new JButton(">");
		String monthName = shownMonth[0].getMonth().toString();
		monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase();
		JLabel monthLabel = new JLabel(monthName + " " + shownMonth[0].getYear(), SwingConstants.CENTER);

		prevButton.addActionListener(e -> {
			shownMonth[0] = shownMonth[0].minusMonths(1);
			refresh[0].run();
		});
		nextButton.addActionListener(e -> {
			shownMonth[0] = shownMonth[0].plusMonths(1);
			refresh[0].run();
		});

		JPanel header = new JPanel(new BorderLayout());
		header.add(prevButton, BorderLayout.WEST);
		header.add(monthLabel, BorderLayout.CENTER);
		header.add(nextButton, BorderLayout.EAST);
		return header;
	}

	/**
	 * Builds a 7-column grid of weekday headers and day buttons for the given month.
	 * Clicking a day fills {@code dateField} with that date and closes {@code popup}.
	 */
	private JPanel createCalendarGrid(YearMonth yearMonth, JTextField dateField, JPopupMenu popup) {
		JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));

		for (String dayName : new String[] { "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su" }) {
			JLabel dayNameLabel = new JLabel(dayName, SwingConstants.CENTER);
			dayNameLabel.setFont(dayNameLabel.getFont().deriveFont(Font.BOLD));
			grid.add(dayNameLabel);
		}

		int leadingBlanks = yearMonth.atDay(1).getDayOfWeek().getValue() - 1;
		for (int i = 0; i < leadingBlanks; i++) {
			grid.add(new JLabel(""));
		}

		LocalDate today = LocalDate.now();
		for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
			LocalDate date = yearMonth.atDay(day);
			JButton dayButton = new JButton(String.valueOf(day));
			dayButton.setMargin(new Insets(2, 2, 2, 2));
			if (date.equals(today)) {
				dayButton.setFont(dayButton.getFont().deriveFont(Font.BOLD));
				dayButton.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215), 2));
			}
			dayButton.addActionListener(e -> {
				dateField.setText(date.toString());
				popup.setVisible(false);
			});
			grid.add(dayButton);
		}

		return grid;
	}

	/**
	 * Adds a label and field side by side on their own row of a {@link GridBagLayout}
	 * form, advancing {@code gbc} to the next row.
	 */
	private void addField(JPanel form, GridBagConstraints gbc, String label, JComponent field) {
		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		form.add(new JLabel(label), gbc);
		gbc.gridx = 1;
		form.add(field, gbc);
		gbc.gridx = 0;
	}

	/**
	 * Adds a label above a field, each on its own row of a {@link GridBagLayout} form,
	 * advancing {@code gbc} to the next free row.
	 */
	private void addFieldStacked(JPanel form, GridBagConstraints gbc, String label, JComponent field) {
		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		form.add(new JLabel(label), gbc);
		gbc.gridy++;
		form.add(field, gbc);
	}

}
