package nus.climods.storage.module.user;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import nus.climods.commons.core.LogsCenter;
import nus.climods.commons.exceptions.DataConversionException;
import nus.climods.commons.exceptions.IllegalValueException;
import nus.climods.commons.util.FileUtil;
import nus.climods.commons.util.JsonUtil;
import nus.climods.logic.commands.exceptions.CommandException;
import nus.climods.model.Model;
import nus.climods.model.module.UniqueUserModuleList;

/**
 * A class to access UserModuleList data stored as a json file on the hard disk.
 */
public class JsonUserModuleListStorage implements UserModuleListStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonUserModuleListStorage.class);

    private final Path filePath;

    private final Model model;

    /**
     * Creates JsonUserModuleListStorage with
     * @param filePath of the stored json
     * @param model of all modules
     */
    public JsonUserModuleListStorage(Path filePath, Model model) {
        this.filePath = filePath;
        this.model = model;
    }

    public Path getUserModuleListFilePath() {
        return filePath;
    }

    @Override
    public Optional<UniqueUserModuleList> readUserModuleList() throws DataConversionException {
        return readUserModuleList(filePath);
    }

    /**
     * Similar to {@link #readUserModuleList()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataConversionException if the file is not in the correct format.
     */
    public Optional<UniqueUserModuleList> readUserModuleList(Path filePath) throws DataConversionException {
        requireNonNull(filePath);

        Optional<JsonSerializableUserModuleList> jsonUserModuleList = JsonUtil.readJsonFile(
            filePath, JsonSerializableUserModuleList.class);
        if (jsonUserModuleList.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonUserModuleList.get().toModelType());
        } catch (IllegalValueException | CommandException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataConversionException(ive);
        }
    }

    @Override
    public void saveUserModuleList(UniqueUserModuleList userModuleList) throws IOException {
        saveUserModuleList(userModuleList, filePath);
    }

    /**
     * Similar to {@link #saveUserModuleList(UniqueUserModuleList)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveUserModuleList(UniqueUserModuleList userModuleList, Path filePath) throws IOException {
        requireNonNull(userModuleList);
        requireNonNull(filePath);

        FileUtil.createIfMissing(filePath);
        JsonUtil.saveJsonFile(new JsonSerializableUserModuleList(userModuleList, model), filePath);
    }

}
