package ru.sibsutis.appointment.core.exception;

public class SlotAlreadyBookedException extends RuntimeException {
  public SlotAlreadyBookedException(String message) {
    super(message);
  }
}
