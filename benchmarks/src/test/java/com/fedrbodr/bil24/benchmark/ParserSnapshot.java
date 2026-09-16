package com.fedrbodr.bil24.benchmark;

record ParserSnapshot(
    long id,
    String sector,
    String sectorName,
    String row,
    String rowName,
    String seat,
    String seatName,
    String fullSector,
    String fullRow,
    String fullSeat) {
  static ParserSnapshot of(test.ResultParser result) {
    if (result == null) {
      return null;
    }
    return new ParserSnapshot(
        result.getId(),
        result.getSector(),
        result.getSectorName(),
        result.getRow(),
        result.getRowName(),
        result.getSeat(),
        result.getSeatName(),
        result.getFullSector(),
        result.getFullRow(),
        result.getFullSeat());
  }

  static ParserSnapshot of(reference.ResultParser result) {
    if (result == null) {
      return null;
    }
    return new ParserSnapshot(
        result.getId(),
        result.getSector(),
        result.getSectorName(),
        result.getRow(),
        result.getRowName(),
        result.getSeat(),
        result.getSeatName(),
        result.getFullSector(),
        result.getFullRow(),
        result.getFullSeat());
  }
}
