module edu.uoc.uoctron {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    exports edu.uoc.uoctron;
    exports edu.uoc.uoctron.view to javafx.fxml;

    opens edu.uoc.uoctron.view to javafx.fxml;


}
