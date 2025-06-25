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

if (!isset($data["correo"])) {
    echo json_encode(["success" => false, "error" => "Correo no recibido"]);
    exit;
}

$correo = $conexion->real_escape_string($data["correo"]);
$resultado = $conexion->query("SELECT * FROM usuarios WHERE correo = '$correo'");

if ($resultado->num_rows > 0) {
    $usuario = $resultado->fetch_assoc();
    echo json_encode(["success" => true, "usuario" => $usuario]);
} else {
    echo json_encode(["success" => false, "error" => "Usuario no encontrado"]);
}
?>
