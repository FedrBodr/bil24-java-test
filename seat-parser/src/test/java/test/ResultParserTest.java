package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ResultParserTest {
  @Test
  void exposesDefaultValuesFromPublicConstructor() {
    ResultParser result = new ResultParser();

    assertEquals(0L, result.getId());
    assertNull(result.getSector());
    assertNull(result.getSectorName());
    assertNull(result.getRow());
    assertNull(result.getRowName());
    assertNull(result.getSeat());
    assertNull(result.getSeatName());
    assertEquals("", result.getFullSector());
    assertEquals("", result.getFullRow());
    assertEquals("", result.getFullSeat());
  }

  @Test
  void settersAndGettersRoundTripAllFields() {
    ResultParser result = new ResultParser();

    result.setId(-42L);
    result.setSector("Ложа");
    result.setSectorName("A");
    result.setRow("Ряд");
    result.setRowName("12");
    result.setSeat("Место");
    result.setSeatName("34");

    assertEquals(-42L, result.getId());
    assertEquals("Ложа", result.getSector());
    assertEquals("A", result.getSectorName());
    assertEquals("Ряд", result.getRow());
    assertEquals("12", result.getRowName());
    assertEquals("Место", result.getSeat());
    assertEquals("34", result.getSeatName());
  }

  @Test
  void fullSectorOmitsSectorWordIgnoringCase() {
    ResultParser result = new ResultParser();
    result.setSector("СеКтОр");
    result.setSectorName("A");

    assertEquals("A", result.getFullSector());
  }

  @Test
  void fullValuesReflectSetterChanges() {
    ResultParser result = new ResultParser();
    result.setSector("Ложа");
    result.setSectorName("A");
    result.setRow("Ряд");
    result.setRowName("1");
    result.setSeat("Место");
    result.setSeatName("2");

    assertEquals("Ложа A", result.getFullSector());
    assertEquals("Ряд 1", result.getFullRow());
    assertEquals("Место 2", result.getFullSeat());

    result.setSectorName("");
    result.setRowName("");
    result.setSeatName("");

    assertEquals("Ложа ", result.getFullSector());
    assertEquals("Ряд ", result.getFullRow());
    assertEquals("Место ", result.getFullSeat());
  }

  @Test
  void fullValuesPreserveEmptyComponents() {
    ResultParser result = new ResultParser();
    result.setSector("");
    result.setSectorName("");
    result.setRow("");
    result.setRowName("");
    result.setSeat("");
    result.setSeatName("");

    assertEquals(" ", result.getFullSector());
    assertEquals(" ", result.getFullRow());
    assertEquals(" ", result.getFullSeat());
  }

  @Test
  void fullValuesReturnEmptyWhenEitherComponentIsNull() {
    ResultParser result = new ResultParser();
    result.setSector("Ложа");
    result.setRow("Ряд");
    result.setSeat("Место");

    assertEquals("", result.getFullSector());
    assertEquals("", result.getFullRow());
    assertEquals("", result.getFullSeat());

    result.setSector(null);
    result.setSectorName("A");
    result.setRow(null);
    result.setRowName("1");
    result.setSeat(null);
    result.setSeatName("2");

    assertEquals("", result.getFullSector());
    assertEquals("", result.getFullRow());
    assertEquals("", result.getFullSeat());
  }
}
