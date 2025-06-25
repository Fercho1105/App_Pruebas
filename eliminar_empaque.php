<?php
header("Content-Type: application/json; charset=utf-8");
ini_set('display_errors', 1);
error_reporting(E_ALL);

$conexion = new mysqli("localhost", "root", "root", "tripmates_db");

if ($conexion->connect_error) {
    echo json_encode(["success" => false, "error" => "Error de conexión"]);
    exit;
}

$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data["id"])) {
    echo json_encode(["success" => false, "error" => "ID no recibido"]);
    exit;
}

$id = intval($data["id"]);

if ($conexion->query("DELETE FROM lista_empaque WHERE id = $id")) {
    echo json_encode(["success" => true]);
} else {
    echo json_encode(["success" => false, "error" => $conexion->error]);
}
?>
