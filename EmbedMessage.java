import java.awt.image.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.imageio.*;
import javax.swing.filechooser.FileFilter;

public class EmbedMessage implements ActionListener {
   JFrame frame = new JFrame("Embedding the message");
   JButton open = new JButton("Open");
   JButton embed = new JButton("Embed");
   JButton save = new JButton("Save");
   JButton reset = new JButton("Reset");
   JTextArea message = new JTextArea(10, 3);
   BufferedImage sourceImage = null, embeddedImage = null;
   JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
   JScrollPane originalPane = new JScrollPane();
   JScrollPane embeddedPane = new JScrollPane();

   public EmbedMessage() {
      frame.setSize(1200, 900);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
      JPanel p = new JPanel(new FlowLayout());
      p.add(open);
      p.add(embed);
      p.add(save);
      p.add(reset);
      frame.getContentPane().add(p, BorderLayout.SOUTH);
      open.addActionListener(this);
      embed.addActionListener(this);
      save.addActionListener(this);

      reset.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            message.setText("");
         }
      });

      p = new JPanel(new GridLayout(1, 1));
      p.add(new JScrollPane(message));

      message.setFont(new Font("Arial", Font.BOLD, 20));
      p.setBorder(BorderFactory.createTitledBorder("Message to be embedded"));
      frame.getContentPane().add(p, BorderLayout.NORTH);
      originalPane.setBorder(BorderFactory.createTitledBorder("Original Image"));
      embeddedPane.setBorder(BorderFactory.createTitledBorder("Steganographed Image"));
      frame.getContentPane().add(sp, BorderLayout.CENTER);
      sp.setLeftComponent(originalPane);
      sp.setRightComponent(embeddedPane);
   }

   public void actionPerformed(ActionEvent ae) {
      Object o = ae.getSource();
      if (o == open)
         openImage();
      else if (o == embed)
         embedMessage();
      else if (o == save)
         saveImage();
   }

   private java.io.File showFileDialog(final boolean open) {
      JFileChooser fc = new JFileChooser("Open an image");
      javax.swing.filechooser.FileFilter ff = new FileFilter() {
         public boolean accept(java.io.File f) {
            String name = f.getName().toLowerCase();
            if (open)
               return f.isDirectory() || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
            return f.isDirectory() || name.endsWith(".png") || name.endsWith(".bmp");
         }

         public String getDescription() {
            if (open)
               return "Image (*.jpg, *.jpeg, *.png)";
            return "Image (*.png)";
         }
      };
      fc.setAcceptAllFileFilterUsed(false);
      fc.addChoosableFileFilter(ff);

      java.io.File f = null;
      if (open && fc.showOpenDialog(frame) == fc.APPROVE_OPTION)
         f = fc.getSelectedFile();
      else if (!open && fc.showSaveDialog(frame) == fc.APPROVE_OPTION)
         f = fc.getSelectedFile();
      return f;
   }

   private void openImage() {
      java.io.File f = showFileDialog(true);
      try {
         sourceImage = ImageIO.read(f);
         JLabel l = new JLabel(new ImageIcon(sourceImage));
         originalPane.getViewport().add(l);
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   private void embedMessage() {
      String mess = message.getText();
      embeddedImage = sourceImage.getSubimage(0, 0, sourceImage.getWidth(), sourceImage.getHeight());
      embedMessageToImage(embeddedImage, mess);
      JLabel l = new JLabel(new ImageIcon(embeddedImage));
      embeddedPane.getViewport().add(l);
   }

   private void embedMessageToImage(BufferedImage img, String mess) {
      int messageLength = mess.length();

      int imageWidth = img.getWidth();
      int imageHeight = img.getHeight();
      int imageSize = imageWidth * imageHeight;
      if (messageLength * 8 + 32 > imageSize) {
         JOptionPane.showMessageDialog(frame, "Message is too long for the chosen image", "Message too long!",
               JOptionPane.ERROR_MESSAGE);
         return;
      }
      embedInteger(img, messageLength, 0, 0);

      byte b[] = mess.getBytes();
      for (int i = 0; i < b.length; i++)
         embedByte(img, b[i], i * 8 + 32, 0);
   }

   private void embedInteger(BufferedImage img, int n, int start, int storageBit) {
      int maxX = img.getWidth();
      int maxY = img.getHeight();
      int startX = start / maxY;
      int startY = start - startX * maxY;
      int count = 0;
      for (int i = startX; i < maxX && count < 32; i++) {
         for (int j = startY; j < maxY && count < 32; j++) {
            int rgb = img.getRGB(i, j);
            int bit = getBitValue(n, count);
            rgb = setBitValue(rgb, storageBit, bit);
            img.setRGB(i, j, rgb);
            count++;
         }
      }
   }

   private void embedByte(BufferedImage img, byte b, int start, int storageBit) {
      int maxX = img.getWidth(), maxY = img.getHeight(), startX = start / maxY, startY = start - startX * maxY,
            count = 0;
      for (int i = startX; i < maxX && count < 8; i++) {
         for (int j = startY; j < maxY && count < 8; j++) {
            int rgb = img.getRGB(i, j), bit = getBitValue(b, count);
            rgb = setBitValue(rgb, storageBit, bit);
            img.setRGB(i, j, rgb);
            count++;
         }
      }
   }

   private void saveImage() {
      if (embeddedImage == null) {
         JOptionPane.showMessageDialog(frame, "No message has been embedded!", "Nothing to save",
               JOptionPane.ERROR_MESSAGE);
         return;
      }
      java.io.File f = showFileDialog(false);
      String name = f.getName();
      String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
      if (!ext.equals("png") && !ext.equals("bmp") && !ext.equals("dib")) {
         ext = "png";
         f = new java.io.File(f.getAbsolutePath() + ".png");
      }
      try {
         if (f.exists())
            f.delete();
         ImageIO.write(embeddedImage, ext.toUpperCase(), f);
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }


   private int getBitValue(int n, int location) {
      int v = n & (int) Math.round(Math.pow(2, location));
      return v == 0 ? 0 : 1;
   }

   private int setBitValue(int n, int location, int bit) {
      int toggle = (int) Math.pow(2, location), bv = getBitValue(n, location);
      if (bv == bit)
         return n;
      if (bv == 0 && bit == 1)
         n |= toggle;
      else if (bv == 1 && bit == 0)
         n ^= toggle;
      return n;
   }

   public static void main(String arg[]) {
      new EmbedMessage();
   }
}