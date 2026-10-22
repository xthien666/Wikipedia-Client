# TV4 integration

Bookmark and history storage are ready. The library uses real saved data. The reader save action and history screen still need to be connected.

## Shared access

Get the container from `WikiApplication` in the Activity/Fragment and pass its repositories and executor to your ViewModel:

```java
AppContainer container = ((WikiApplication) requireActivity()
        .getApplication()).getContainer();
```

Run all repository calls on `container.getDiskExecutor()`. They reject main-thread access. Catch failures in the ViewModel and update UI state on the main thread. Do not close the shared database from a screen.

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

After the full article is displayed successfully, capture the time and call `recordRead(article, displayedAt)` through `container.getHistoryRepository()` on the disk executor. Use epoch milliseconds. Do not record history on a search click, summary response, failed load, or screen recreation alone.

## TV1 — history screen

Replace the sample list with `container.getHistoryRepository()`:

- `getHistory()` — returns `List<PersonalArticleEntry>`, newest first.
- `removeHistory("vi", pageId)` — delete one entry.
- `clearHistory()` — delete all history after confirmation.

Each entry exposes `getArticle()` and `getTimestamp()` (last-read time). Reload after deletion and when returning from the reader. Open a row on the main thread using the existing navigator:

```java
PersonalArticle article = entry.getArticle();
ArticleNavigator.open(this, new ArticleLocator(
        article.getLanguage(), article.getTitle(), article.getPageId()));
```

Here, `this` is the history Fragment. `PersonalArticle` and `PersonalArticleEntry` are in `data.model`; `ArticleNavigator` is in `ui.navigation`.

## Storage behavior

- Identity is `language + pageId`, not the title. Only `vi` is supported.
- Saving again updates metadata and keeps the original saved time.
- Reading again updates the existing history row; older timestamps are ignored.
- Clearing history does not remove bookmarks.
- Only metadata is stored. Reading articles still requires a network connection.
