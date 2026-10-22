# TV4 integration

Bookmark and history storage are integrated with TV1's history UI. The library uses real saved data. The reader save action and successful-read callback still need to be connected by TV3.

## Shared access

Get the container from `WikiApplication` in the Activity/Fragment and pass its repositories and executor to your ViewModel:

```java
AppContainer container = ((WikiApplication) requireActivity()
        .getApplication()).getContainer();
```

BookmarkRepository is blocking: run its calls on `container.getDiskExecutor()`, catch failures in the ViewModel and publish UI state on the main thread. HistoryRepository is asynchronous: it schedules disk operations internally and exposes LiveData. Do not close the shared database from a screen.

## TV3 — reader

Use metadata from the final API response after redirects:

```java
PersonalArticle article = new PersonalArticle(
        "vi", resolvedPageId, resolvedTitle, canonicalUrl);
```

Through `container.getBookmarkRepository()`:

- `isBookmarked("vi", pageId)` — load the saved state.
- `saveBookmark(article, System.currentTimeMillis())` — save the article.
- `removeBookmark("vi", pageId)` — remove it.

Keep the article identity attached to each operation. If the user switches articles before it finishes, do not apply the old result to the new article's save button.

After the full article is displayed successfully, capture the time and call `container.getHistoryRepository().recordRead(article, displayedAt)`. This call schedules its own disk work. Use epoch milliseconds. Do not record history on a search click, summary response, failed load, or screen recreation alone. Observe history state for write failures.

## TV1 — history screen

The history UI already uses `container.getHistoryRepository()`:

- `observe()` — LiveData of immutable HistoryState snapshots, newest first.
- `refresh()` — reload on entering the history screen.
- `retry()` — repeat the last failed write/removal, or retry a failed read. A refresh does not silently dismiss a pending write failure. A newer explicit write supersedes the failed action; retry state is in memory for the current process.
- `remove("vi", pageId)` — asynchronously delete one entry.
- `clear()` — asynchronously delete all history after confirmation.

Each HistoryEntry exposes resolved metadata, `getLastReadAt()` and `toLocator()`. Writes publish a new snapshot automatically. Open a row on the main thread using the existing navigator:

```java
ArticleNavigator.open(this, entry.toLocator());
```

Here, `this` is the history Fragment. HistoryEntry is in `data.model`; ArticleNavigator is in `ui.navigation`. SqliteHistoryStore contains TV4's blocking history operations; screens use the asynchronous HistoryRepository facade.

## Storage behavior

- Identity is `language + pageId`, not the title. Only `vi` is supported.
- Saving again updates metadata and keeps the original saved time.
- Reading again updates the existing history row; older timestamps are ignored.
- Clearing history does not remove bookmarks.
- Only metadata is stored. Reading articles still requires a network connection.
- TV1's legacy SharedPreferences history is validated and imported in a transaction. The old entries are removed only after import succeeds; malformed input remains available for retry or an explicitly confirmed reset.
