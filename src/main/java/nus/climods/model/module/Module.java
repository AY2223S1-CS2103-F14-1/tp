package nus.climods.model.module;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openapitools.client.ApiException;
import org.openapitools.client.api.ModulesApi;
import org.openapitools.client.model.Lesson;
import org.openapitools.client.model.ModuleInformationSemesterDataInner;
import org.openapitools.client.model.SemesterData;
import org.openapitools.client.model.SemestersEnum;

/**
 * A wrapper class for <code>ModuleInformation</code>
 */
public class Module {

    private final org.openapitools.client.model.ModuleInformation apiModuleInfo;
    private String academicYear;
    private HashMap<SemestersEnum, Set<LessonType>> lessonTypesMap;
    // Semester -> LessonType -> LessonId -> [Lesson]
    private HashMap<SemestersEnum, HashMap<LessonType, ModuleLessonIdMap>> lessonMap;

    /**
     * Contains detailed module information from API. Only initialised when needed
     */
    private org.openapitools.client.model.Module apiModule;
    private boolean isFocused = false;

    static class ModuleLessonIdMap extends HashMap<String, List<Lesson>> {

    }

    /**
     * Constructor for Module.
     *
     * @param apiModuleInfo ModuleInformation from nusmods API
     */
    public Module(org.openapitools.client.model.ModuleInformation apiModuleInfo) {
        this.apiModuleInfo = apiModuleInfo;
    }

    /**
     * Constructor for Module.
     *
     * @param apiModuleInfo ModuleInformation from nusmods API
     * @param academicYear  academic year
     */
    public Module(org.openapitools.client.model.ModuleInformation apiModuleInfo, String academicYear) {
        this(apiModuleInfo);
        this.academicYear = academicYear;
    }

    /**
     * Initialize module lesson data
     */
    private void setLessonData() {
        requireNonNull(apiModule);

        lessonMap = new HashMap<>();
        lessonTypesMap = new HashMap<>();

        List<SemesterData> moduleSemesterData = apiModule.getSemesterData();
        for (SemesterData semesterData : moduleSemesterData) {
            SemestersEnum semester = SemestersEnum.fromValue(semesterData.getSemester());
            List<Lesson> semesterLessons = semesterData.getTimetable();
            if (semesterLessons == null) {
                semesterLessons = Collections.emptyList();
            }

            // Set lesson types for each semester
            Set<LessonType> semesterLessonsType =
                semesterLessons.stream().map(Lesson::getLessonType).map(LessonType::fromValue)
                    .collect(Collectors.toSet());
            lessonTypesMap.put(semester, semesterLessonsType);

            // Set lessons for each semester
            HashMap<LessonType, ModuleLessonIdMap> semesterLessonMap = new HashMap<>();
            semesterLessons.forEach(semesterLesson -> {
                String lessonId = semesterLesson.getClassNo();
                LessonType lessonType = LessonType.fromValue(semesterLesson.getLessonType());
                if (!semesterLessonMap.containsKey(lessonType)) {
                    semesterLessonMap.put(lessonType, new ModuleLessonIdMap());
                }
                if (!semesterLessonMap.get(lessonType).containsKey(lessonId)) {
                    semesterLessonMap.get(lessonType).put(lessonId, new ArrayList<>());
                }
                semesterLessonMap.get(lessonType).get(lessonId).add(semesterLesson);
            });
            lessonMap.put(semester, semesterLessonMap);
        }
    }

    /**
     * Returns the number of modular credits.
     * <p>
     * String is used as the return type as there module credits may not be a integer. Example: YSC2251 2.5 MCs Since
     * there are no calculations needed as of yet, we will preserve the String representation
     * </p>
     *
     * @return module credits
     */
    public String getModuleCredit() {
        return apiModuleInfo.getModuleCredit();
    }

    /**
     * Returns the module title.
     *
     * @return module title
     */
    public String getTitle() {
        return apiModuleInfo.getTitle();
    }

    /**
     * Returns the module code.
     *
     * @return module code
     */
    public String getCode() {
        return apiModuleInfo.getModuleCode();
    }

    /**
     * Returns the department that offers this module.
     *
     * @return module department
     */
    public String getDepartment() {
        return apiModuleInfo.getDepartment();
    }

    /**
     * Returns the module description
     *
     * @return module description
     */
    public String getDescription() {
        return apiModuleInfo.getDescription();
    }

    /**
     * Returns the semesters that this module is offered.
     *
     * @return list of semesters
     */
    public List<SemestersEnum> getSemesters() {
        List<ModuleInformationSemesterDataInner> apiSemesterData = apiModuleInfo.getSemesterData();

        return apiSemesterData.stream().map(ModuleInformationSemesterDataInner::getSemester).filter(Objects::nonNull)
            .map(SemestersEnum::fromValue).collect(Collectors.toList());
    }

    /**
     * Checks if module is offered in a specified semester
     *
     * @param semester semester
     * @return true if module is offered else false
     */
    public boolean availableInSemester(SemestersEnum semester) {
        return getSemesters().stream().anyMatch(availableSemester -> availableSemester.equals(semester));
    }

    /**
     * Returns the lesson types in module
     *
     * @param semester semester
     * @return lesson types
     */
    public Set<LessonType> getLessonTypes(SemestersEnum semester) {
        requireNonNull(lessonTypesMap);
        assert lessonTypesMap.containsKey(semester);

        return lessonTypesMap.get(semester);
    }

    /**
     * Checks if module offers a lesson type
     *
     * @param lessonType lesson type
     * @return true if module offers the lesson type else false
     */
    public boolean hasLessonType(LessonType lessonType) {
        requireNonNull(lessonTypesMap);
        return lessonTypesMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet()).contains(lessonType);
    }

    /**
     * Checks if module offers a lesson type
     *
     * @param lessonType lesson type
     * @param semester   semester
     * @return true if module offers the lesson type else false
     */
    public boolean hasLessonType(LessonType lessonType, SemestersEnum semester) {
        requireNonNull(lessonTypesMap);
        return lessonTypesMap.get(semester).contains(lessonType);
    }

    /**
     * Returns the lessons types that can be selected
     * <p>
     * Only lessons that have more than one slot (categorized by the class number) is considered selectable
     * </p>
     *
     * @param semester semester
     * @return a set of lesson types
     */
    public Set<LessonType> getSelectableLessonTypes(SemestersEnum semester) {
        requireNonNull(lessonMap);
        assert lessonMap.containsKey(semester);

        Predicate<Map.Entry<LessonType, ModuleLessonIdMap>> selectableLessonPredicate =
            lessonEntry -> lessonEntry.getValue().size() > 1;

        return lessonMap.get(semester).entrySet().stream().filter(selectableLessonPredicate).map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Returns the lesson types that cannot be selected
     *
     * @param semester semester
     * @return a set of lesson types
     */
    public Set<LessonType> getUnselectableLessonTypes(SemestersEnum semester) {
        Set<LessonType> unselectableSet = getLessonTypes(semester);
        unselectableSet.removeAll(getSelectableLessonTypes(semester));

        return unselectableSet;
    }

    /**
     * Checks if a lesson type is selectable in module
     *
     * @param lessonType lesson type
     * @param semester   semester
     * @return true if lesson type is selectable else false
     */
    public boolean isLessonTypeSelectable(LessonType lessonType, SemestersEnum semester) {
        return getSelectableLessonTypes(semester).contains(lessonType);
    }

    /**
     * Returns the lessons offered by module categorized by lesson type
     *
     * @param semester semester
     * @return lessons
     */
    public HashMap<LessonType, ModuleLessonIdMap> getLessons(SemestersEnum semester) {
        requireNonNull(lessonMap);
        assert lessonMap.containsKey(semester);

        return lessonMap.get(semester);
    }

    /**
     * Check if module has a specified lesson id
     *
     * @param lessonId   lesson id
     * @param semester   semester
     * @param lessonType lesson type
     * @return true if module has lesson id else false
     */
    public boolean hasLessonId(String lessonId, SemestersEnum semester, LessonType lessonType) {
        requireNonNull(lessonMap);

        return Optional.of(lessonMap.get(semester)).map(semesterMap -> semesterMap.get(lessonType))
            .map(semesterLessonMap -> semesterLessonMap.get(lessonId)).isPresent();
    }

    /**
     * Returns String lessonId that is unselectable by user, such as fixed Lecture or recitation slots
     * @param lessonType
     * @param semester
     * @return unselectableLessonId
     */
    public String getUnselectableLessonId(LessonType lessonType, SemestersEnum semester) {
        requireNonNull(lessonMap);

        //safe to assume that keySet only has one element in it because lesson is unselectable.
        return Optional.of(lessonMap.get(semester)).map(semesterMap -> semesterMap.get(lessonType))
                .map(lessonSet -> lessonSet.keySet().toArray()[0]).get().toString();
    }

    /**
     * Returns the description of the module's preclusion.
     *
     * @return module preclusion description
     */
    public String getPreclusion() {
        requireNonNull(apiModule);
        return apiModule.getPreclusion();
    }

    /**
     * Returns the description of the module's Prerequisite.
     *
     * @return module Prerequisite description
     */
    public String getPrerequisite() {
        requireNonNull(apiModule);
        return apiModule.getPrerequisite();
    }

    /**
     * Check if module contains keyword.
     * <p>
     * A keyword is searched against a search range which includes the module's title and code
     * </p>
     *
     * @param keywordPattern keyword regex pattern
     * @return whether module contains keyword in its stated information
     */
    public boolean containsKeyword(Pattern keywordPattern) {
        List<String> searchRange = Arrays.asList(getCode(), getTitle());

        return searchRange.stream().anyMatch(range -> keywordPattern.asPredicate().test(range));
    }

    /**
     * Load more module data.
     * <p>
     * This could trigger a API call to retrieve more data for the module. This side effect only happens when module
     * data is not cached locally.
     * </p>
     *
     * @throws ApiException if api call fails
     */
    public void loadMoreData() throws ApiException {
        if (apiModule == null) {
            apiModule = ModulesApi.getInstance().acadYearModulesModuleCodeJsonGet(academicYear, getCode());
            setLessonData();
        }
    }

    /**
     * Returns the focused state of the module.
     *
     * @return true if module is in focused else false
     */
    public boolean isFocused() {
        return isFocused;
    }

    /**
     * Clear the focused state of module
     */
    public void clearFocus() {
        isFocused = false;
    }

    /**
     * Focus on module.
     *
     * @throws ApiException if api call fails
     */
    public void requestFocus() throws ApiException {
        loadMoreData();
        isFocused = true;
    }
}
