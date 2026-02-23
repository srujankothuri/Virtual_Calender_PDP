package calendar.controller.export;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Factory dispatch should be based on filename extension. */
public class ExportFormatterFactoryTest {

  /** CSV extension maps to {@link CsvExportFormatter}. */
  @Test
  public void returnsCsvFormatterForCsv() {
    ExportFormatter f = ExportFormatterFactory.getFormatter("data.csv");
    assertTrue(f instanceof CsvExportFormatter);
  }

  /** ICS/ICAL extensions map to {@link IcalExportFormatter}. */
  @Test
  public void returnsIcalFormatterForIcsOrIcal() {
    ExportFormatter f1 = ExportFormatterFactory.getFormatter("data.ics");
    ExportFormatter f2 = ExportFormatterFactory.getFormatter("data.ical");
    assertTrue(f1 instanceof IcalExportFormatter);
    assertTrue(f2 instanceof IcalExportFormatter);
  }

  /** Unsupported extensions should throw. */
  @Test(expected = IllegalArgumentException.class)
  public void throwsForUnsupportedExtension() {
    ExportFormatterFactory.getFormatter("x.txt");
  }
}
