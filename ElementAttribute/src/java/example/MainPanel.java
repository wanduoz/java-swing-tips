// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public final class MainPanel extends JPanel {
  private static final String[] HTML_TEXT = {
    "<html><body>",
    "span tag: <span style='background:#88ff88;' title='tooltip: span[@title]'>span span span</span><br />",
    "<div title='tooltip: div[@title]'>div tag: div div div div</div>",
    "<div style='padding: 2 24;'><img src='",
    Objects.toString(MainPanel.class.getResource("favicon.png")),
    "' alt='16x16 favicon' />&nbsp;",
    "<a href='https://ateraimemo.com/' title='Title: JST'>Java Swing Tips</a></div>",
    "</body></html>"
  };
  private String tooltip;

  private MainPanel() {
    super(new BorderLayout());
    String html = String.join("\n", HTML_TEXT);

    JEditorPane editor1 = new CustomTooltipEditorPane();
    editor1.setEditorKit(new HTMLEditorKit());
    editor1.setText(html);
    editor1.setEditable(false);
    ToolTipManager.sharedInstance().registerComponent(editor1);

    JEditorPane editor2 = new JEditorPane();
    editor2.setEditorKit(new TooltipEditorKit());
    editor2.setText(html);
    editor2.setEditable(false);
    editor2.addHyperlinkListener(e -> {
      JEditorPane editorPane = (JEditorPane) e.getSource();
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        JOptionPane.showMessageDialog(editorPane, "You click the link with the URL " + e.getURL());
      } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
        tooltip = editorPane.getToolTipText();
        editorPane.setToolTipText(Optional.ofNullable(e.getURL()).map(URL::toExternalForm).orElse(null));
        // URL url = e.getURL();
        // editorPane.setToolTipText(Objects.nonNull(url) ? url.toExternalForm() : null);
      } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
        editorPane.setToolTipText(tooltip);
      }
    });
    ToolTipManager.sharedInstance().registerComponent(editor2);

    JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    sp.setResizeWeight(.5);
    sp.setTopComponent(new JScrollPane(editor1));
    sp.setBottomComponent(new JScrollPane(editor2));
    add(sp);
    setPreferredSize(new Dimension(320, 240));
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(MainPanel::createAndShowGui);
  }

  private static void createAndShowGui() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
      ex.printStackTrace();
      Toolkit.getDefaultToolkit().beep();
    }
    JFrame frame = new JFrame("@title@");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

class CustomTooltipEditorPane extends JEditorPane {
  private final transient Position.Bias[] bias = new Position.Bias[1];
  private transient HyperlinkListener listener;
  // private boolean doesElementContainLocation(JEditorPane editor, Element e, int offset, int x, int y) {
  //   if (e != null && offset > 0 && e.getStartOffset() == offset) {
  //     try {
  //       TextUI ui = editor.getUI();
  //       Shape s1 = ui.modelToView(editor, offset, Position.Bias.Forward);
  //       if (s1 == null) {
  //         return false;
  //       }
  //       Rectangle r1 = s1 instanceof Rectangle ? (Rectangle) s1 : s1.getBounds();
  //       Shape s2 = ui.modelToView(editor, e.getEndOffset(), Position.Bias.Backward);
  //       if (s2 != null) {
  //         Rectangle r2 = s2 instanceof Rectangle ? (Rectangle) s2 : s2.getBounds(); r1.add(r2);
  //       }
  //       return r1.contains(x, y);
  //     } catch (BadLocationException ex) {
  //       throw new RuntimeException(ex); // should never happen
  //     }
  //   }
  //   return true;
  // }

  @Override public void updateUI() {
    removeHyperlinkListener(listener);
    super.updateUI();
    listener = new HyperlinkListener() {
      private String tooltip;
      @Override public void hyperlinkUpdate(HyperlinkEvent e) {
        JEditorPane editor = (JEditorPane) e.getSource();
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          JOptionPane.showMessageDialog(editor, e.getURL());
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
          tooltip = editor.getToolTipText();
          Optional.ofNullable(e.getSourceElement())
            .map(elem -> (AttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A))
            .ifPresent(attr -> editor.setToolTipText(Objects.toString(attr.getAttribute(HTML.Attribute.TITLE))));
          // Element elem = e.getSourceElement();
          // if (Objects.nonNull(elem)) {
          //   AttributeSet attr = elem.getAttributes();
          //   AttributeSet a = (AttributeSet) attr.getAttribute(HTML.Tag.A);
          //   if (Objects.nonNull(a)) {
          //     editor.setToolTipText((String) a.getAttribute(HTML.Attribute.TITLE));
          //   }
          // }
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
          editor.setToolTipText(tooltip);
        }
      }
    };
    addHyperlinkListener(listener);
  }

  @Override public String getToolTipText(MouseEvent e) {
    String title = super.getToolTipText(e);
    JEditorPane editor = (JEditorPane) e.getComponent();
    if (!editor.isEditable()) {
      int pos = editor.getUI().viewToModel(editor, e.getPoint(), bias);
      // Java 9: int pos = editor.getUI().viewToModel2D(editor, e.getPoint(), bias);
      if (bias[0] == Position.Bias.Backward && pos > 0) {
        pos--;
      }
      if (pos >= 0 && editor.getDocument() instanceof HTMLDocument) {
        HTMLDocument doc = (HTMLDocument) editor.getDocument();
        return getSpanTitleAttribute(doc, pos).orElse(title);
      }
    }
    return title;
  }

  private Optional<String> getSpanTitleAttribute(HTMLDocument doc, int pos) {
    // HTMLDocument doc = (HTMLDocument) editor.getDocument();
    Element elem = doc.getCharacterElement(pos);
    // if (!doesElementContainLocation(editor, elem, pos, e.getX(), e.getY())) {
    //   elem = null;
    // }
    // if (elem != null) {
    AttributeSet a = elem.getAttributes();
    AttributeSet span = (AttributeSet) a.getAttribute(HTML.Tag.SPAN);
    return Optional.ofNullable(span).map(s -> Objects.toString(s.getAttribute(HTML.Attribute.TITLE)));
  }
}

class TooltipEditorKit extends HTMLEditorKit {
  @Override public ViewFactory getViewFactory() {
    return new HTMLFactory() {
      @Override public View create(Element elem) {
        AttributeSet attrs = elem.getAttributes();
        Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
        Object o = Objects.isNull(elementName) ? attrs.getAttribute(StyleConstants.NameAttribute) : null;
        if (o instanceof HTML.Tag) {
          HTML.Tag kind = (HTML.Tag) o;
          if (kind == HTML.Tag.DIV) {
            return new BlockView(elem, View.Y_AXIS) {
              @Override public String getToolTipText(float x, float y, Shape allocation) {
                String s = super.getToolTipText(x, y, allocation);
                if (Objects.isNull(s)) {
                  s = Objects.toString(getElement().getAttributes().getAttribute(HTML.Attribute.TITLE));
                }
                return s;
              }
            };
          }
        }
        return super.create(elem);
      }
    };
  }
}
