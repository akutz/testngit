package com.kutzbuilders.testngit;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * <p>
 * This class is a TestNG IAnnotationTransformer that enables/disables tests
 * based on the currently checked out branch. If the name of the currently
 * checked out branch matches one of the groups defined in the test annotation's
 * groups property then the test is enabled, otherwise it is disabled provided
 * the currently checked out branch is not the branch defined in the system
 * property below.
 * </p>
 * 
 * <p>
 * Users can pass a system property, <b>testngit.itbranches</b>, that specifies
 * the name of the branch(es) for which all tests should run regardless of
 * whether or not the tests have a group defined that equals the name of the
 * currently checked out branch. It is possible to specify multiple branch names
 * by separating them with a comma.
 * </p>
 * 
 * <p>
 * Additionally, in the case of a build environment where the build agents do
 * not directly check out the source code and therefore may not have access to
 * the git repository information, it is possible to specific a system property
 * that supersedes checking the file system for the currently checked out
 * branch. Users can pass in the system property, <b>testngit.currentBranch</b>,
 * to specify the name of the current branch instead of attempting to read the
 * information for the local Git repository.
 * 
 * @author akutz
 * 
 */
public class TestNGAnnotationBranchEnabler implements IAnnotationTransformer
{
    private final static Logger log = LoggerFactory
        .getLogger(TestNGAnnotationBranchEnabler.class);

    @Override
    public void transform(
        ITestAnnotation annotation,
        @SuppressWarnings("rawtypes") Class testClass,
        @SuppressWarnings("rawtypes") Constructor testConstructor,
        Method testMethod)
    {
        String testName =
            testMethod != null ? testMethod.getName() : testClass
                .getSimpleName();

        log.info("transforming test annotation f/ '{}'", testName);

        if (!annotation.getEnabled())
        {
            log.info(
                "test='{}' is already disabled; not inspecting branch",
                testName);
            return;
        }

        String branchName = getCurrentBranch();
        if (branchName == null)
        {
            log.error("unable to read current branch");
            return;
        }
        
        log.info("currently checked out branch='{}'", branchName);

        String[] groups = annotation.getGroups();
        log.info("annotation groups={} f/ test='{}'", groups, testName);

        String itBranchesStr = System.getProperty("testngit.itbranches", null);
        log.info("testng.itbranches='{}'", itBranchesStr);

        String[] itBranches = null;
        if (itBranchesStr != null)
        {
            itBranches = itBranchesStr.split(",");
        }

        List<String> validBranches =
            new ArrayList<String>((groups == null ? 0 : groups.length)
                + (itBranches == null ? 0 : itBranches.length));

        if (groups != null)
        {
            validBranches.addAll(Arrays.asList(groups));
        }

        if (itBranches != null)
        {
            validBranches.addAll(Arrays.asList(itBranches));
        }

        if (!validBranches.contains(branchName))
        {
            log.info("disabled test='{}'", testName);
            annotation.setEnabled(false);
        }
    }

    private String getCurrentBranch()
    {
        String branchName = System.getProperty("testngit.currentBranch", null);

        if (branchName != null)
        {
            log.info(
                "read branch name='{}' f/ sysprop 'testngit.currentBranch'",
                branchName);
            return branchName;
        }

        // Get the name of the currently checked out branch.
        FileRepositoryBuilder gitBuilder = new FileRepositoryBuilder();
        Repository repo;

        try
        {
            repo = gitBuilder.findGitDir().build();
        }
        catch (IOException e)
        {
            log.error("error finding git directory", e);
            return null;
        }

        try
        {
            branchName = repo.getBranch();
            log.info("currently checked out branch='{}'", branchName);
        }
        catch (IOException e)
        {
            log.error("error getting branch name", e);
            return null;
        }

        return branchName;
    }
}
