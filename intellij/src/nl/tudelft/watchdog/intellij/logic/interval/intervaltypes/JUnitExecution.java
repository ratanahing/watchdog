package nl.tudelft.watchdog.intellij.logic.interval.intervaltypes;

import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo.Magnitude;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.JUnitExecutionBase;
import nl.tudelft.watchdog.core.logic.network.JsonifiedDouble;
import nl.tudelft.watchdog.intellij.logic.ui.listeners.JUnitListener;
import nl.tudelft.watchdog.intellij.util.WatchDogUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * JUnit execution representation in a tree-structure.
 */
public class JUnitExecution extends JUnitExecutionBase {

    /**
     * Result states of a test.
     */
    enum Result {
        UNDEFINED("Undefined"),
        OK("OK"),
        ERROR("Error"),
        FAILURE("Failure"),
        IGNORED("Ignored");

        private String name;

        Result(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    /**
     * JUnitExecution
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public JUnitExecution(AbstractTestProxy testProxy, JUnitExecution parent) {
        double elapsedTime = testProxy.getDuration() / 1000.0;
        if (elapsedTime > 0.0) {
            duration = new JsonifiedDouble(elapsedTime);
        }

        setResult(determineTestResult(testProxy));

        if (parent == null) {
            // Root of the test run session
            setProjectNameHash(JUnitListener.getProject(testProxy).getName());

            if (testProxy.getChildren().size() > 1 && testProxy.getChildren().get(0).isLeaf()) {
                // If test run session is a class with more than 1 test method:
                // Wrap that class one level below (consistent with Eclipse)
                childrenExecutions = new ArrayList<JUnitExecutionBase>();
                childrenExecutions.add(new JUnitExecution(testProxy,this));
            } else {
                childrenExecutions = createTree(testProxy);
            }
            return;
        }

        if (testProxy.isLeaf()) {
            // Test case (test method)
            String[] testNames = testProxy.getName().split(Pattern.quote("."));

            if (testNames.length > 1) {
                // IntelliJ15 compatible call
                parent.setClassNameHash(testNames[0]);
                testMethodHash = WatchDogUtils.createHash(testNames[1]);
            } else {
                // IntelliJ14 compatible call
                parent.setClassNameHash(testProxy.getParent().getName());
                testMethodHash = WatchDogUtils.createHash(testProxy.getName());
            }
        } else {
            // Test container (test class with test methods)
            childrenExecutions = createTree(testProxy);
        }

    }

    /**
     * @return The Result of the Test
     */
    private Result determineTestResult(AbstractTestProxy test) {
        if (test.isPassed()) { // getMagnitude() <= PoolOfTestStates.PASSED_INDEX
            return Result.OK;
        } else if (test.getMagnitude() == Magnitude.IGNORED_INDEX.getValue()) {
            return Result.IGNORED;
        } else if (test.getMagnitude() == Magnitude.ERROR_INDEX.getValue()) {
            return Result.ERROR;
        } else if (test.getMagnitude() == Magnitude.FAILED_INDEX.getValue()) {
            return Result.FAILURE;
        } else return Result.UNDEFINED;
    }

    /**
     * Sets the result.
     */
    public void setResult(Result result) {
        this.result = result.toString().substring(0, 1);
    }

    private ArrayList<JUnitExecutionBase> createTree(AbstractTestProxy session) {
        ArrayList<JUnitExecutionBase> children = new ArrayList<JUnitExecutionBase>();
        for (AbstractTestProxy testChild : session.getChildren()) {
            children.add(new JUnitExecution(testChild, this));
        }
        return children;
    }
}
