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

if (!isset($data["usuario_id"]) || !isset($data["viaje_id"]) || !isset($data["rol"]) || !isset($data["fecha_actualizacion"])) {
    echo json_encode(["success" => false, "error" => "Datos incompletos"]);
    exit;
}

$usuario_id = intval($data["usuario_id"]);
$viaje_id = intval($data["viaje_id"]);
$rol = $conexion->real_escape_string($data["rol"]);
$fecha_actualizacion = $conexion->real_escape_string($data["fecha_actualizacion"]);

$consulta = $conexion->query("SELECT * FROM viajes_usuarios WHERE usuario_id = $usuario_id AND viaje_id = $viaje_id");

if ($consulta->num_rows > 0) {
    $conexion->query("UPDATE viajes_usuarios SET rol = '$rol', fecha_actualizacion = '$fecha_actualizacion' WHERE usuario_id = $usuario_id AND viaje_id = $viaje_id");
} else {
    $conexion->query("INSERT INTO viajes_usuarios (usuario_id, viaje_id, rol, fecha_actualizacion) VALUES ($usuario_id, $viaje_id, '$rol', '$fecha_actualizacion')");
}

echo json_encode(["success" => true]);
?>
