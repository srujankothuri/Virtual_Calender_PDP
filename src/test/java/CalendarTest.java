import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/** Centralized test suite to run all calendar tests. */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    calendar.util.DateTimesTest.class,
    calendar.view.ConsoleViewTest.class,
    calendar.view.SwingCalendarViewTest.class,
    calendar.view.CalendarViewModelTest.class,
    calendar.model.EventTest.class,
    calendar.model.CalendarServiceTest.class,
    calendar.controller.export.ExportFormatterFactoryTest.class,
    calendar.controller.services.ExportServiceTest.class,
    calendar.controller.commands.ExportCommandTest.class,
    calendar.controller.commands.CommandInvokerTest.class
})
public class CalendarTest {

}
