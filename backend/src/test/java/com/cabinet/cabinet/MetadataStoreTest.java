package com.cabinet.cabinet;

import com.cabinet.model.FileRecord;
import com.cabinet.storage.MetadataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataStoreTest {

	@TempDir
	Path tempDir;

	@Test
	void saveThenFindAll_putRecordIn_readItBack_assertFieldsMatch() {
		MetadataStore store = newStore();
		FileRecord record = new FileRecord("notes", 123L, "abc123", Instant.parse("2026-05-14T10:15:30Z"), Instant.parse("2026-05-14T10:15:30Z"));

		store.save("notes", record);

		Map<String, FileRecord> all = store.findAll();
		assertEquals(1, all.size());
		assertEquals(record, all.get("notes"));
	}

	@Test
	void saveOverwrites_insertSameNameTwice_onlySecondVersionRemains() {
		MetadataStore store = newStore();
		FileRecord first = new FileRecord("same-name", 1L, "first", Instant.parse("2026-05-14T10:00:00Z"), Instant.parse("2026-05-14T10:00:00Z"));
		FileRecord second = new FileRecord("same-name", 2L, "second", Instant.parse("2026-05-14T11:00:00Z"), Instant.parse("2026-05-14T11:00:00Z"));

		store.save("same-name", first);
		store.save("same-name", second);

		Map<String, FileRecord> all = store.findAll();
		assertEquals(1, all.size());
		assertEquals(second, all.get("same-name"));
	}

	@Test
	void delete_saveThenDelete_confirmItsGoneFromFindAll() {
		MetadataStore store = newStore();
		FileRecord record = new FileRecord("to-delete", 10L, "md5", Instant.parse("2026-05-14T12:00:00Z"), Instant.parse("2026-05-14T12:00:00Z"));

		store.save("to-delete", record);
		store.delete("to-delete");

		Map<String, FileRecord> all = store.findAll();
		assertFalse(all.containsKey("to-delete"));
		assertTrue(all.isEmpty());
	}

	@Test
	void findAllOnEmptyStore_returnsEmptyMapNotNullOrException() {
		MetadataStore store = newStore();

		Map<String, FileRecord> all = store.findAll();

		assertNotNull(all);
		assertTrue(all.isEmpty());
	}

	@Test
	void persistence_saveThenReload_newStoreSeesSameData() {
		MetadataStore store = newStore();
		FileRecord record = new FileRecord("persist-me", 777L, "persisted", Instant.parse("2026-05-14T13:00:00Z"), Instant.parse("2026-05-14T13:00:00Z"));

		store.save("persist-me", record);

		MetadataStore reloaded = newStore();
		Optional<FileRecord> loaded = reloaded.find("persist-me");

		assertTrue(loaded.isPresent());
		assertEquals(record, loaded.get());
	}

	private MetadataStore newStore() {
		MetadataStore store = new MetadataStore();
		setStorageDir(store);
		store.load();
		return store;
	}

	private void setStorageDir(MetadataStore store) {
		try {
			Field field = MetadataStore.class.getDeclaredField("storageDir");
			field.setAccessible(true);
			field.set(store, tempDir.toString());
		} catch (Exception e) {
			throw new RuntimeException("Failed to set storageDir", e);
		}
	}
}
