<?php
header("Content-Type: application/json");
ini_set('display_errors', 1);
error_reporting(E_ALL);

$conexion = new mysqli("localhost", "root", "root", "tripmates_db");
if ($conexion->connect_error) {
    echo json_encode(["success" => false, "error" => "Error de conexión"]);
    exit;
}

$data = json_decode(file_get_contents("php://input"), true);
if (!$data || !isset($data["usuario_id"]) || !isset($data["relaciones"])) {
    echo json_encode(["success" => false, "error" => "Datos incompletos"]);
    exit;
}

$idUsuario = intval($data["usuario_id"]);
$relacionesCliente = $data["relaciones"];

// Procesar relaciones del cliente
foreach ($relacionesCliente as $relacion) {
    $usuario_id = intval($relacion["usuario_id"]);
    $viaje_id = intval($relacion["viaje_id"]);
    $rol = $conexion->real_escape_string($relacion["rol"]);
    $fecha = $conexion->real_escape_string($relacion["fecha_actualizacion"]);

    $query = "SELECT fecha_actualizacion FROM viajes_usuarios WHERE usuario_id = $usuario_id AND viaje_id = $viaje_id";
    $res = $conexion->query($query);

    if ($res->num_rows > 0) {
        $fila = $res->fetch_assoc();
        if ($fila["fecha_actualizacion"] < $fecha) {
            $conexion->query("UPDATE viajes_usuarios SET rol = '$rol', fecha_actualizacion = '$fecha' WHERE usuario_id = $usuario_id AND viaje_id = $viaje_id");
        }
    } else {
        $conexion->query("INSERT INTO viajes_usuarios (usuario_id, viaje_id, rol, fecha_actualizacion) VALUES ($usuario_id, $viaje_id, '$rol', '$fecha')");
    }
}

// Enviar relaciones del servidor
$relacionesServidor = [];
$resultado = $conexion->query("SELECT * FROM viajes_usuarios WHERE usuario_id = $idUsuario");

while ($fila = $resultado->fetch_assoc()) {
    $relacionesServidor[] = $fila;
}

echo json_encode(["success" => true, "relaciones" => $relacionesServidor]);
?>
