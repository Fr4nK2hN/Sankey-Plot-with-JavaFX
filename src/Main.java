import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends Application {
    private static final double MAX_NODE_HEIGHT = 50;
    private LinkedHashMap<String, Double> data;
    private Group group;
    private Pane pane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        pane = new Pane();
        group = new Group();

        data = new LinkedHashMap<>();

        double initialWidth = 700;
        double initialHeight = 600;

        Button fileChooserButton = new Button("Choose File");
        fileChooserButton.setOnAction(_ -> {
            try {
                handleFileSelection(primaryStage, initialWidth, initialHeight);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        VBox vbox = new VBox(fileChooserButton, pane);
        vbox.setAlignment(Pos.CENTER);


        Scene scene = new Scene(vbox, initialWidth, initialHeight);


        primaryStage.setTitle("Sankey Plot");
        primaryStage.setScene(scene);
        primaryStage.show();


    }

    private void handleFileSelection(Stage primaryStage, double initialWidth, double initialHeight) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Data File");
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String title = reader.readLine();
                String sourceNodeName = reader.readLine();

                data.clear();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ");

                    if (parts.length >= 2) {
                        String key = parts[0];
                        for (int i = 1; i < parts.length - 1; i++) {
                            key += STR." \{parts[i]}";
                        }

                        double value = Double.parseDouble(parts[parts.length - 1]);
                        data.put(key, value);
                    } else {
                        throw new ArrayIndexOutOfBoundsException("Invalid data format in the file.");
                    }
                }
                reader.close();

                updateVisualization(primaryStage, title, sourceNodeName, initialWidth, initialHeight);
            } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                ex.printStackTrace();
                showErrorAlert();
            }
        }
    }

    private void showErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Processing File");
        alert.setHeaderText(null);
        alert.setContentText("There was an error processing the selected file. Please check the file format.");
        alert.showAndWait();
    }

    private void updateVisualization(Stage primaryStage, String title, String sourceNodeName, double initialWidth, double initialHeight) {
        group.getChildren().clear();

        double sum = 0;
        for (double value : data.values()) {
            sum += value;
        }

        Rectangle sourceNode = createNode(100, 100, 50, sum, Color.LIGHTGREEN, sourceNodeName);
        group.getChildren().add(sourceNode);

        ArrayList<Rectangle> endNodes = createRectangleNodes(data, group);

        Path path = drawCurves(sourceNode, endNodes);
        group.getChildren().add(path);
        group.getChildren().addAll(endNodes);

        pane.getChildren().add(group);
        Text text = new Text(STR."\{sourceNodeName}: \{sum}");
        text.setFill(Color.BLACK);
        text.setLayoutX(160);
        text.setLayoutY(100 + sourceNode.getHeight() / 2);
        group.getChildren().add(text);

        group.translateXProperty().bind(pane.widthProperty().divide(2).subtract(group.getBoundsInParent().getWidth() / 1.5));
        group.translateYProperty().bind(pane.heightProperty().divide(2).subtract(group.getBoundsInParent().getHeight() / 1.5));

        Text titleLabel = new Text(title);
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setFill(Color.BLACK);
        titleLabel.setLayoutX((sourceNode.getX()));
        titleLabel.setLayoutY(sourceNode.getY() + sourceNode.getHeight() + 30);
        group.getChildren().add(titleLabel);

        VBox vbox = new VBox(pane);
        vbox.setAlignment(Pos.CENTER);

        Scene scene = new Scene(vbox, pane.getWidth(), pane.getHeight());
        primaryStage.setScene(scene);

        group.scaleXProperty().bind(Bindings.min(scene.widthProperty().divide(initialWidth), scene.heightProperty().divide(initialHeight)));
        group.scaleYProperty().bind(Bindings.min(scene.heightProperty().divide(initialHeight), scene.widthProperty().divide(initialWidth)));
        pane.scaleXProperty().bind(Bindings.min(scene.widthProperty().divide(initialWidth), scene.heightProperty().divide(initialHeight)));
        pane.scaleYProperty().bind(Bindings.min(scene.heightProperty().divide(initialHeight), scene.widthProperty().divide(initialWidth)));


    }

    private Rectangle createNode(double x, double y, double width, double value, Color color, String id) {
        double scaledHeight = (value / getMaxDataValue()) * MAX_NODE_HEIGHT;
        Rectangle rectangle = new Rectangle(x, y, width, scaledHeight);
        rectangle.setFill(color);
        rectangle.setId(id);
        return rectangle;
    }

    private ArrayList<Rectangle> createRectangleNodes(LinkedHashMap<String, Double> data, Group group) {
        ArrayList<Rectangle> nodes = new ArrayList<>();
        double numberOfSpaces = data.size();
        double startX = 500;
        double startY = 100 - (numberOfSpaces * 20) / 2;
        double width = 30;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            Rectangle node = createNode(startX, startY, width, entry.getValue(), Color.LIGHTSKYBLUE, entry.getKey());
            nodes.add(node);

            Text text = new Text(STR."\{entry.getKey()}: \{entry.getValue()}");
            text.setFill(Color.BLACK);
            text.setLayoutX(startX + width + 10);
            text.setLayoutY(startY + node.getHeight() / 2);
            group.getChildren().add(text);

            startY += node.getHeight() + 20;
        }
        return nodes;
    }

    private Path drawCurves(Rectangle sourceNode, ArrayList<Rectangle> targets) {
        double startX = sourceNode.getX() + sourceNode.getWidth();
        double startY = sourceNode.getY();

        Path path = new Path();
        path.setFillRule(FillRule.EVEN_ODD);
        path.setStroke(Color.TRANSPARENT);
        path.setStrokeWidth(0);
        path.setFill(Color.LIGHTBLUE);

        for (Rectangle target : targets) {
            double endX = target.getX();
            double endY = target.getY();

            MoveTo moveTo = new MoveTo(startX, startY);
            path.getElements().add(moveTo);

            CubicCurveTo curveTo1 = createCubicCurveTo(startX, startY, endX, endY);
            path.getElements().add(curveTo1);

            LineTo lineTo1 = new LineTo(endX, endY + target.getHeight());
            path.getElements().add(lineTo1);

            CubicCurveTo curveTo2 = createCubicCurveTo(endX, endY + target.getHeight(), startX, startY + target.getHeight());
            path.getElements().add(curveTo2);

            LineTo lineTo2 = new LineTo(startX, startY);
            path.getElements().add(lineTo2);

            startY += target.getHeight();
        }

        return path;
    }

    private CubicCurveTo createCubicCurveTo(double startX, double startY, double endX, double endY) {
        return new CubicCurveTo(
                startX + (endX - startX) / 3,
                startY,
                endX - (endX - startX) / 3,
                endY,
                endX,
                endY
        );
    }

    private double getMaxDataValue() {
        return data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
    }
}
