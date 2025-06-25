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

if (!$data || !isset($data["correo"]) || !isset($data["nombre"]) || !isset($data["contraseña"])) {
    echo json_encode(["success" => false, "error" => "Datos incompletos"]);
    exit;
}

$correo = $conexion->real_escape_string($data["correo"]);
$nombre = $conexion->real_escape_string($data["nombre"]);
$contrasena = $conexion->real_escape_string($data["contraseña"]);
$fecha = date("Y-m-d H:i:s");

// Verificar si ya existe
$verificar = $conexion->query("SELECT id FROM usuarios WHERE correo = '$correo'");
if ($verificar->num_rows > 0) {
    echo json_encode(["success" => false, "error" => "Correo ya registrado"]);
    exit;
}

// Insertar
$conexion->query("INSERT INTO usuarios (correo, nombre, contrasena, fecha_actualizacion)
                  VALUES ('$correo', '$nombre', '$contrasena', '$fecha')");

$idInsertado = $conexion->insert_id;

echo json_encode([
    "success" => true,
    "usuario" => [
        "id" => $idInsertado,
        "correo" => $correo,
        "nombre" => $nombre,
        "contrasena" => $contrasena,
        "fecha_actualizacion" => $fecha
    ]
]);
exit;
?>
