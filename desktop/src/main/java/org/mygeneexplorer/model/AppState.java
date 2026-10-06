package org.mygeneexplorer.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;

/**
 * Observable state of the main window: the model of the MVC triad.
 *
 * <p>The view binds its controls to these properties; only the controller changes them, except
 * for {@link #queryProperty() the query}, which the view edits as the user types.
 */
public final class AppState {

    private final StringProperty query = new SimpleStringProperty(this, "query", "");
    private final BooleanProperty queryInvalid = new SimpleBooleanProperty(this, "queryInvalid");
    private final BooleanProperty loading = new SimpleBooleanProperty(this, "loading");
    private final ObjectProperty<SearchResult> result =
            new SimpleObjectProperty<>(this, "result");
    private final ObjectProperty<StatusMessage> status =
            new SimpleObjectProperty<>(this, "status", StatusMessage.info("status.ready"));
    private final IntegerProperty fontSize = new SimpleIntegerProperty(this, "fontSize");
    private final IntegerProperty maxVariants = new SimpleIntegerProperty(this, "maxVariants");
    private final ObservableList<String> recentSearches = FXCollections.observableArrayList();
    private final ObservableList<String> recentSearchesView =
            FXCollections.unmodifiableObservableList(recentSearches);

    /** @return the text of the search field, editable by the view */
    public StringProperty queryProperty() {
        return query;
    }

    /** @return the text of the search field */
    public String getQuery() {
        return query.get();
    }

    /** @param value new text of the search field */
    public void setQuery(String value) {
        query.set(value);
    }

    /** @return whether the query was rejected, so the view can highlight the search field */
    public ReadOnlyBooleanProperty queryInvalidProperty() {
        return queryInvalid;
    }

    /** @return whether the query was rejected */
    public boolean isQueryInvalid() {
        return queryInvalid.get();
    }

    /** @param value whether the query was rejected */
    public void setQueryInvalid(boolean value) {
        queryInvalid.set(value);
    }

    /** @return whether a search is running */
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading;
    }

    /** @return whether a search is running */
    public boolean isLoading() {
        return loading.get();
    }

    /** @param value whether a search is running */
    public void setLoading(boolean value) {
        loading.set(value);
    }

    /** @return the displayed result, {@code null} when none */
    public ReadOnlyObjectProperty<SearchResult> resultProperty() {
        return result;
    }

    /** @return the displayed result, {@code null} when none */
    public SearchResult getResult() {
        return result.get();
    }

    /** @param value result to display, {@code null} to show the welcome screen */
    public void setResult(SearchResult value) {
        result.set(value);
    }

    /** @return the message of the status bar */
    public ReadOnlyObjectProperty<StatusMessage> statusProperty() {
        return status;
    }

    /** @return the message of the status bar */
    public StatusMessage getStatus() {
        return status.get();
    }

    /** @param value new message of the status bar */
    public void setStatus(StatusMessage value) {
        status.set(value);
    }

    /** @return the base text size of the interface, in pixels */
    public ReadOnlyIntegerProperty fontSizeProperty() {
        return fontSize;
    }

    /** @return the base text size of the interface, in pixels */
    public int getFontSize() {
        return fontSize.get();
    }

    /** @param value base text size of the interface, in pixels */
    public void setFontSize(int value) {
        fontSize.set(value);
    }

    /** @return the maximum number of variants fetched per search */
    public ReadOnlyIntegerProperty maxVariantsProperty() {
        return maxVariants;
    }

    /** @return the maximum number of variants fetched per search */
    public int getMaxVariants() {
        return maxVariants.get();
    }

    /** @param value maximum number of variants fetched per search */
    public void setMaxVariants(int value) {
        maxVariants.set(value);
    }

    /** @return the recent searches, most recent first (read-only view) */
    public ObservableList<String> getRecentSearches() {
        return recentSearchesView;
    }

    /** @param searches new recent searches, most recent first */
    public void setRecentSearches(Collection<String> searches) {
        recentSearches.setAll(searches);
    }
}
