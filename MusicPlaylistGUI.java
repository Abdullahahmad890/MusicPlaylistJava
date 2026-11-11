import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MusicPlaylistGUI extends JFrame {
    private Process backend;
    private BufferedReader backendOutput;
    private BufferedWriter backendInput;

    private JTextField idField = new JTextField(8);
    private JTextField titleField = new JTextField(12);
    private JTextField artistField = new JTextField(12);
    private JTextField durationField = new JTextField(6);
    private JTextField searchField = new JTextField(12);
    private JComboBox<String> searchType = new JComboBox<>(new String[]{"ID", "Title"});

    private DefaultTableModel model = new DefaultTableModel(
        new String[]{"Song ID", "Title", "Artist", "Duration (sec)"}, 0
    ) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private JTable table = new JTable(model);
    private JLabel currentLabel = new JLabel("Current song: None");
    private JLabel countLabel = new JLabel("Total songs: 0");

    public MusicPlaylistGUI() {
        setTitle("Music Playlist Manager");
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            startBackend();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not start playlist_backend.exe. Compile the C++ file first.\n" + ex.getMessage(),
                "Backend error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        buildWindow();
        refreshPlaylist();
        updateCurrent();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try { send("EXIT"); backend.destroy(); } catch (Exception ignored) {}
                dispose();
                System.exit(0);
            }
        });
    }

    private void startBackend() throws IOException {
        File folder = new File(System.getProperty("user.dir"));
        File exe = new File(folder, "playlist_backend.exe");
        if (!exe.exists()) {
            // Also try the folder where this Java class is being run from.
            exe = new File("playlist_backend.exe");
        }
        backend = new ProcessBuilder(exe.getAbsolutePath()).directory(folder).start();
        backendInput = new BufferedWriter(new OutputStreamWriter(
            backend.getOutputStream(), StandardCharsets.UTF_8));
        backendOutput = new BufferedReader(new InputStreamReader(
            backend.getInputStream(), StandardCharsets.UTF_8));
    }

    private String send(String command) throws IOException {
        backendInput.write(command);
        backendInput.newLine();
        backendInput.flush();
        String response = backendOutput.readLine();
        if (response == null) throw new IOException("The C++ backend stopped unexpectedly.");
        return response;
    }

    private String enc(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String dec(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    private void buildWindow() {
        JPanel top = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add a song"));
        addPanel.add(new JLabel("ID:")); addPanel.add(idField);
        addPanel.add(new JLabel("Title:")); addPanel.add(titleField);
        addPanel.add(new JLabel("Artist:")); addPanel.add(artistField);
        addPanel.add(new JLabel("Duration (sec):")); addPanel.add(durationField);
        JButton addButton = new JButton("Add Song");
        addButton.addActionListener(e -> addSong());
        addPanel.add(addButton);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search / Remove"));
        searchPanel.add(searchType);
        searchPanel.add(searchField);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchSong());
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSong());
        searchPanel.add(searchButton);
        searchPanel.add(removeButton);

        top.add(addPanel);
        top.add(searchPanel);

        table.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(table);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout());
        JButton previousButton = new JButton("Previous");
        previousButton.addActionListener(e -> navigate("PREVIOUS"));
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(e -> navigate("NEXT"));
        JButton refreshButton = new JButton("Refresh Playlist");
        refreshButton.addActionListener(e -> refreshPlaylist());
        controls.add(previousButton);
        controls.add(nextButton);
        controls.add(refreshButton);
        bottom.add(controls, BorderLayout.NORTH);

        JPanel status = new JPanel(new GridLayout(2, 1));
        status.add(currentLabel);
        status.add(countLabel);
        bottom.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addSong() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            String title = titleField.getText().trim();
            String artist = artistField.getText().trim();
            int duration = Integer.parseInt(durationField.getText().trim());
            if (title.isEmpty() || artist.isEmpty() || duration <= 0) {
                throw new IllegalArgumentException();
            }
            String response = send("ADD|" + id + "|" + enc(title) + "|" + enc(artist) + "|" + duration);
            showResponse(response);
            if (response.startsWith("OK")) {
                idField.setText(""); titleField.setText(""); artistField.setText(""); durationField.setText("");
                refreshPlaylist();
                updateCurrent();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric ID and duration.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Title and artist cannot be empty, and duration must be positive.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void searchSong() {
        try {
            String value = searchField.getText().trim();
            if (value.isEmpty()) return;
            String response;
            if (searchType.getSelectedItem().equals("ID")) {
                response = send("SEARCH_ID|" + Integer.parseInt(value));
            } else {
                response = send("SEARCH_TITLE|" + enc(value));
            }
            if (response.startsWith("SONG|")) {
                String[] p = response.split("\\|");
                JOptionPane.showMessageDialog(this,
                    "ID: " + p[1] + "\nTitle: " + dec(p[2]) +
                    "\nArtist: " + dec(p[3]) + "\nDuration: " + p[4] + " seconds",
                    "Song found", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Song not found.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric ID.");
        } catch (Exception ex) { showError(ex); }
    }

    private void removeSong() {
        try {
            String value = searchField.getText().trim();
            if (value.isEmpty()) return;
            String command = searchType.getSelectedItem().equals("ID")
                ? "REMOVE_ID|" + Integer.parseInt(value)
                : "REMOVE_TITLE|" + enc(value);
            String response = send(command);
            showResponse(response);
            refreshPlaylist();
            updateCurrent();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric ID.");
        } catch (Exception ex) { showError(ex); }
    }

    private void navigate(String direction) {
        try {
            String response = send(direction);
            if (response.startsWith("SONG|")) {
                String[] p = response.split("\\|");
                currentLabel.setText("Current song: " + dec(p[2]) + " — " + dec(p[3]));
            } else {
                currentLabel.setText("Current song: None");
                JOptionPane.showMessageDialog(this, "The playlist is empty.");
            }
        } catch (Exception ex) { showError(ex); }
    }

    private void refreshPlaylist() {
        try {
            model.setRowCount(0);
            String response = send("LIST");
            String[] p = response.split("\\|");
            for (int i = 1; i + 3 < p.length; i += 4) {
                model.addRow(new Object[]{p[i], dec(p[i + 1]), dec(p[i + 2]), p[i + 3]});
            }
            String count = send("COUNT");
            countLabel.setText("Total songs: " + count.split("\\|")[1]);
        } catch (Exception ex) { showError(ex); }
    }

    private void updateCurrent() {
        try {
            String response = send("CURRENT");
            if (response.startsWith("SONG|")) {
                String[] p = response.split("\\|");
                currentLabel.setText("Current song: " + dec(p[2]) + " — " + dec(p[3]));
            } else {
                currentLabel.setText("Current song: None");
            }
        } catch (Exception ex) { showError(ex); }
    }

    private void showResponse(String response) {
        String[] p = response.split("\\|", 2);
        if (p.length > 1) JOptionPane.showMessageDialog(this, p[1]);
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MusicPlaylistGUI().setVisible(true));
    }
}
