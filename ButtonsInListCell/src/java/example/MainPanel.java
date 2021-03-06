// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

public final class MainPanel extends JPanel {
  private MainPanel() {
    super(new BorderLayout());

    DefaultListModel<String> model = new DefaultListModel<>();
    model.addElement("11\n1");
    model.addElement("222222222222222\n222222222222222");
    model.addElement("3333333333333333333\n33333333333333333333\n33333333333333333");
    model.addElement("444");

    add(new JScrollPane(new JList<String>(model) {
      private transient MouseInputListener cbml;
      @Override public void updateUI() {
        removeMouseListener(cbml);
        removeMouseMotionListener(cbml);
        super.updateUI();
        setFixedCellHeight(-1);
        cbml = new CellButtonsMouseListener<>(this);
        addMouseListener(cbml);
        addMouseMotionListener(cbml);
        setCellRenderer(new ButtonsRenderer<>(model));
      }
    }));
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

class CellButtonsMouseListener<E> extends MouseInputAdapter {
  private int prevIndex = -1;
  private JButton prevButton;
  private final JList<E> list;

  protected CellButtonsMouseListener(JList<E> list) {
    super();
    this.list = list;
  }

  @Override public void mouseMoved(MouseEvent e) {
    // JList<?> list = (JList<?>) e.getComponent();
    Point pt = e.getPoint();
    int index = list.locationToIndex(pt);
    if (!list.getCellBounds(index, index).contains(pt)) {
      if (prevIndex >= 0) {
        rectRepaint(list, list.getCellBounds(prevIndex, prevIndex));
      }
      // index = -1;
      prevButton = null;
      return;
    }
    if (index >= 0) {
      ListCellRenderer<? super E> lcr = list.getCellRenderer();
      if (!(lcr instanceof ButtonsRenderer)) {
        return;
      }
      ButtonsRenderer<?> renderer = (ButtonsRenderer<?>) lcr;
      JButton button = getButton(list, pt, index);
      renderer.button = button;
      if (Objects.nonNull(button)) {
        button.getModel().setRollover(true);
        renderer.rolloverIndex = index;
        if (!button.equals(prevButton)) {
          rectRepaint(list, list.getCellBounds(prevIndex, index));
        }
      } else {
        renderer.rolloverIndex = -1;
        Rectangle r;
        if (prevIndex == index) {
          r = Objects.nonNull(prevButton) ? list.getCellBounds(prevIndex, prevIndex) : null;
        } else {
          r = list.getCellBounds(index, index);
        }
        rectRepaint(list, r);
        prevIndex = -1;
      }
      prevButton = button;
    }
    prevIndex = index;
  }

  @Override public void mousePressed(MouseEvent e) {
    // JList<?> list = (JList<?>) e.getComponent();
    Point pt = e.getPoint();
    int index = list.locationToIndex(pt);
    if (index >= 0) {
      JButton button = getButton(list, pt, index);
      ListCellRenderer<? super E> renderer = list.getCellRenderer();
      if (Objects.nonNull(button) && renderer instanceof ButtonsRenderer) {
        ButtonsRenderer<?> r = (ButtonsRenderer<?>) renderer;
        r.pressedIndex = index;
        r.button = button;
        rectRepaint(list, list.getCellBounds(index, index));
      }
    }
  }

  @Override public void mouseReleased(MouseEvent e) {
    // JList<?> list = (JList<?>) e.getComponent();
    Point pt = e.getPoint();
    int index = list.locationToIndex(pt);
    if (index >= 0) {
      JButton button = getButton(list, pt, index);
      ListCellRenderer<? super E> renderer = list.getCellRenderer();
      if (Objects.nonNull(button) && renderer instanceof ButtonsRenderer) {
        ButtonsRenderer<?> r = (ButtonsRenderer<?>) renderer;
        r.pressedIndex = -1;
        r.button = null;
        button.doClick();
        rectRepaint(list, list.getCellBounds(index, index));
      }
    }
  }

  private static void rectRepaint(JComponent c, Rectangle rect) {
    Optional.ofNullable(rect).ifPresent(c::repaint);
  }

  private static <E> JButton getButton(JList<E> list, Point pt, int index) {
    E prototype = list.getPrototypeCellValue();
    Component c = list.getCellRenderer().getListCellRendererComponent(list, prototype, index, false, false);
    Rectangle r = list.getCellBounds(index, index);
    c.setBounds(r);
    // c.doLayout(); // may be needed for other layout managers (eg. FlowLayout) // *1
    pt.translate(-r.x, -r.y);
    // Component b = SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y);
    // if (b instanceof JButton) {
    //   return (JButton) b;
    // } else {
    //   return null;
    // }
    return Optional.ofNullable(SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y))
        .filter(JButton.class::isInstance).map(JButton.class::cast).orElse(null);
  }
}

class ButtonsRenderer<E> extends JPanel implements ListCellRenderer<E> {
  private static final Color EVEN_COLOR = new Color(0xE6_FF_E6);
  private final JTextArea textArea = new JTextArea();
  private final JButton deleteButton = new JButton("delete");
  private final JButton copyButton = new JButton("copy");
  private final List<JButton> buttons = Arrays.asList(deleteButton, copyButton);
  private int targetIndex;
  protected int pressedIndex = -1;
  protected int rolloverIndex = -1;
  protected JButton button;

  protected ButtonsRenderer(DefaultListModel<E> model) {
    super(new BorderLayout()); // *1
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
    setOpaque(true);
    textArea.setLineWrap(true);
    textArea.setOpaque(false);
    add(textArea);

    deleteButton.addActionListener(e -> {
      boolean isMoreThanOneItem = model.getSize() > 1;
      if (isMoreThanOneItem) {
        model.remove(targetIndex);
      }
    });
    copyButton.addActionListener(e -> model.add(targetIndex, model.get(targetIndex)));

    Box box = Box.createHorizontalBox();
    buttons.forEach(b -> {
      b.setFocusable(false);
      b.setRolloverEnabled(false);
      box.add(b);
      box.add(Box.createHorizontalStrut(5));
    });
    add(box, BorderLayout.EAST);
  }

  @Override public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    d.width = 0; // VerticalScrollBar as needed
    return d;
  }

  @Override public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
    textArea.setText(Objects.toString(value, ""));
    this.targetIndex = index;
    if (isSelected) {
      setBackground(list.getSelectionBackground());
      textArea.setForeground(list.getSelectionForeground());
    } else {
      setBackground(index % 2 == 0 ? EVEN_COLOR : list.getBackground());
      textArea.setForeground(list.getForeground());
    }
    buttons.forEach(ButtonsRenderer::resetButtonStatus);
    if (Objects.nonNull(button)) {
      if (index == pressedIndex) {
        button.getModel().setSelected(true);
        button.getModel().setArmed(true);
        button.getModel().setPressed(true);
      } else if (index == rolloverIndex) {
        button.getModel().setRollover(true);
      }
    }
    return this;
  }

  private static void resetButtonStatus(AbstractButton button) {
    ButtonModel model = button.getModel();
    model.setRollover(false);
    model.setArmed(false);
    model.setPressed(false);
    model.setSelected(false);
  }
}
