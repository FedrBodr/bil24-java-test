package test;

public class ResultParser {
  private long id;
  private String sector;
  private String sectorName;
  private String row;
  private String rowName;
  private String seat;
  private String seatName;

  public ResultParser() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getSector() {
    return sector;
  }

  public void setSector(String sector) {
    this.sector = sector;
  }

  public String getSectorName() {
    return sectorName;
  }

  public void setSectorName(String sectorName) {
    this.sectorName = sectorName;
  }

  public String getRow() {
    return row;
  }

  public void setRow(String row) {
    this.row = row;
  }

  public String getRowName() {
    return rowName;
  }

  public void setRowName(String rowName) {
    this.rowName = rowName;
  }

  public String getSeat() {
    return seat;
  }

  public void setSeat(String seat) {
    this.seat = seat;
  }

  public String getSeatName() {
    return seatName;
  }

  public void setSeatName(String seatName) {
    this.seatName = seatName;
  }

  public String getFullSector() {
    if (sector == null || sectorName == null) {
      return "";
    }
    return sector.equalsIgnoreCase("сектор") ? sectorName : sector + " " + sectorName;
  }

  public String getFullRow() {
    return row == null || rowName == null ? "" : row + " " + rowName;
  }

  public String getFullSeat() {
    return seat == null || seatName == null ? "" : seat + " " + seatName;
  }
}
