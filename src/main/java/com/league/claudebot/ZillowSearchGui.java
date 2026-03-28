package com.league.claudebot;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.List;

public class ZillowSearchGui extends JFrame {

    private final ZillowApiClient apiClient;

    private JTextField keywordField;
    private JComboBox<String> typeCombo;
    private JButton searchButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private static final String[] COLUMNS = {
        "Address", "City", "State", "Price", "Beds", "Baths", "Sq Ft", "Type", "Days Listed", "Status"
    };

    private List<Property> currentProperties;

    public ZillowSearchGui() {
        this.apiClient = new ZillowApiClient();
        initUI();
    }

    private void initUI() {
        setTitle("ClaudeBot - Zillow Property Search");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        add(buildSearchPanel(), BorderLayout.NORTH);
        add(buildResultsPanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.setBackground(new Color(30, 50, 80));

        JLabel titleLabel = new JLabel("Zillow Search");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel kwLabel = new JLabel("Keyword:");
        kwLabel.setForeground(Color.WHITE);

        keywordField = new JTextField(20);
        keywordField.setToolTipText("e.g. Miami FL, Seattle WA, 90210");

        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setForeground(Color.WHITE);

        typeCombo = new JComboBox<>(new String[]{"forSale", "forRent", "sold"});

        searchButton = new JButton("Search");
        searchButton.setBackground(new Color(0, 120, 200));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(100, 20));

        searchButton.addActionListener(e -> performSearch());
        keywordField.addActionListener(e -> performSearch());

        panel.add(titleLabel);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(kwLabel);
        panel.add(keywordField);
        panel.add(typeLabel);
        panel.add(typeCombo);
        panel.add(searchButton);
        panel.add(progressBar);

        return panel;
    }

    private JScrollPane buildResultsPanel() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setRowHeight(24);
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setGridColor(new Color(220, 220, 220));

        // Right-align numeric columns
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int col : new int[]{3, 4, 5, 6, 8}) {
            resultsTable.getColumnModel().getColumn(col).setCellRenderer(rightAlign);
        }

        // Alternate row colors
        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 255));
                }
                if (column == 3) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (column == 4 || column == 5 || column == 6 || column == 8) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        // Double-click opens listing in browser
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentProperties != null) {
                    int row = resultsTable.getSelectedRow();
                    if (row >= 0 && row < currentProperties.size()) {
                        openUrl(currentProperties.get(row).getUrl());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        statusLabel = new JLabel("Enter a keyword to search for properties.");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel hint = new JLabel("  |  Double-click a row to open listing in browser.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);

        panel.add(statusLabel);
        panel.add(hint);
        return panel;
    }

    private void performSearch() {
        String keyword = keywordField.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search keyword.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String type = (String) typeCombo.getSelectedItem();

        searchButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Searching...");
        tableModel.setRowCount(0);

        SwingWorker<ZillowApiClient.SearchResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ZillowApiClient.SearchResult doInBackground() throws Exception {
                return apiClient.search(keyword, type);
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                try {
                    ZillowApiClient.SearchResult result = get();
                    currentProperties = result.getProperties();
                    populateTable(currentProperties);
                    statusLabel.setText(String.format("Showing %d of %d total results for \"%s\"",
                            currentProperties.size(), result.getTotalResults(), keyword));
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ZillowSearchGui.this,
                            "Search failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void populateTable(List<Property> properties) {
        tableModel.setRowCount(0);
        for (Property p : properties) {
            tableModel.addRow(new Object[]{
                p.getStreet(),
                p.getCity(),
                p.getState(),
                p.getFormattedPrice(),
                p.getBeds() > 0 ? p.getBeds() : "—",
                p.getBaths() > 0 ? String.format("%.1f", p.getBaths()) : "—",
                p.getArea() > 0 ? String.format("%.0f", p.getArea()) : "—",
                formatHomeType(p.getHomeType()),
                p.getDaysOnZillow(),
                formatStatus(p.getStatus())
            });
        }
    }

    private String formatHomeType(String type) {
        if (type == null) return "";
        return type.replace("_", " ").toLowerCase()
                   .substring(0, 1).toUpperCase()
                + type.replace("_", " ").toLowerCase().substring(1);
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "FOR_SALE" -> "For Sale";
            case "FOR_RENT" -> "For Rent";
            case "SOLD" -> "Sold";
            default -> status;
        };
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not open browser:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
