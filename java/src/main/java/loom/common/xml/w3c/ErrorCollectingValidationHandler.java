package loom.common.xml.w3c;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class ErrorCollectingValidationHandler implements ErrorHandler {
  @Getter private final List<SAXParseException> exceptions = new ArrayList<>();

  @Override
  public void warning(SAXParseException exception) throws SAXException {
    exceptions.add(exception);
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    exceptions.add(exception);
  }

  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    exceptions.add(exception);
  }
}
