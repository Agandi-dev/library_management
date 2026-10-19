package library;

import java.io.File;

/**
 * The outcome of a {@link Library#importMedia(File)} call: how many rows were
 * added to the catalog, and how many were skipped (malformed or duplicate).
 */
public class ImportResult {
	private final int addedCount;
	private final int skippedCount;

	/**
	 * @param addedCount the number of media successfully added
	 * @param skippedCount the number of rows skipped
	 */
	public ImportResult(int addedCount, int skippedCount) {
		this.addedCount = addedCount;
		this.skippedCount = skippedCount;
	}

	/**
	 * @return the number of media successfully added
	 */
	public int getAddedCount() {
		return addedCount;
	}

	/**
	 * @return the number of rows skipped
	 */
	public int getSkippedCount() {
		return skippedCount;
	}
}
