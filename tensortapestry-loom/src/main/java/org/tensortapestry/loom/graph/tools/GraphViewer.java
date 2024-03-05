package org.tensortapestry.loom.graph.tools;

import java.awt.*;
import java.awt.event.*;

import lombok.*;
import org.tensortapestry.loom.graph.LoomGraph;
import org.tensortapestry.loom.graph.dialects.tensorops.ApplicationExpressionDialect;
import org.tensortapestry.loom.graph.dialects.tensorops.OperationExpressionDialect;

public class GraphViewer {

  @Data
  @AllArgsConstructor
  public static class DoublePoint {

    public double x;
    public double y;
  }

  public static class ImageComponent extends Canvas {

    private final Image image;

    private final DoublePoint origin = new DoublePoint(0, 0);

    @Setter
    private double zoomFactor = 1.0;

    private Point prevPt;

    public ImageComponent(Image image) {
      this.image = image;

      addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            prevPt = e.getPoint();
          }
        }
      );

      addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
            Point currentPt = e.getPoint();
            origin.x += (currentPt.x - prevPt.x);
            origin.y += (currentPt.y - prevPt.y);
            prevPt = currentPt;
            repaint();
          }
        }
      );

      addMouseWheelListener(e -> {
        /*
        // Zoom in/out ignoring mouse position
        zoomFactor *= 1 + 0.1 * e.getWheelRotation();
        zoomFactor = Math.max(0.1, zoomFactor); // minimum zoom factor
        repaint();
         */

        // var oldZoom = zoomFactor;
        zoomFactor *= 1 + 0.1 * e.getWheelRotation();

        /*
        // Zoom in/out around mouse position
        // FIXME: broken
        var mouse = e.getPoint();
        var mouseInImage = new Point((int) (mouse.x * oldZoom), (int) (mouse.y * oldZoom));
        var mouseInImageNew = new Point((int) (mouse.x * newZoom), (int) (mouse.y * newZoom));
        origin.x += (mouseInImage.x - mouseInImageNew.x);
        origin.y += (mouseInImage.y - mouseInImageNew.y);
         */

        repaint();
      });
    }

    public void scaleToFit() {
      scaleToFit(getHeight(), getWidth());
    }

    public void pack() {
      setSize(
        (int) (image.getWidth(this) * zoomFactor),
        (int) (image.getHeight(this) * zoomFactor)
      );
    }

    public void scaleToFit(int height, int width) {
      var initialScaleFactor = GraphViewer.calculateScaleFactorToFit(image, height, width);
      setZoomFactor(initialScaleFactor);
      repaint();
    }

    @Override
    public void paint(Graphics g) {
      // TODO: double buffering
      var g2 = (Graphics2D) g;
      g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR
      );
      g2.scale(zoomFactor, zoomFactor);
      g2.drawImage(image, (int) (origin.x / zoomFactor), (int) (origin.y / zoomFactor), this);
    }
  }

  public static void graphViewer(LoomGraph graph) {
    // Get the screen size
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    var initialHeight = (int) (screenSize.getHeight() * 0.8);
    var initialWidth = (int) (screenSize.getWidth() * 0.8);

    Image img = OperationExpressionDialect.toImage(graph);
    var imgComponent = new ImageComponent(img);
    imgComponent.setSize(initialWidth, initialHeight);
    imgComponent.scaleToFit();
    imgComponent.pack();

    var frame = new Frame();

    frame.setLayout(new BorderLayout());
    frame.add(imgComponent, BorderLayout.CENTER);
    frame.pack();
    frame.setLocationRelativeTo(null); // Centers the frame on the screen
    frame.setVisible(true);

    frame.addKeyListener(
      new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
          }
        }
      }
    );

    frame.addWindowListener(
      new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent we) {
          System.exit(0);
        }
      }
    );
  }

  public static double calculateScaleFactorToFit(Image image, int height, int width) {
    // Adjust frame size to account for insets

    // Get the dimensions of the image
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);

    // Calculate scale factors for width and height
    double scaleFactorWidth = (double) width / imageWidth;
    double scaleFactorHeight = (double) height / imageHeight;

    // The scale factor should be the minimum of the two to fit the image entirely within the frame
    return Math.min(scaleFactorWidth, scaleFactorHeight);
  }
}
