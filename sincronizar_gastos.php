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

if (!isset($data["id_viaje"]) || !isset($data["gastos"])) {
    echo json_encode(["success" => false, "error" => "Datos incompletos"]);
    exit;
}

$idViaje = intval($data["id_viaje"]);
$gastosCliente = $data["gastos"];
$gastosServidor = [];

foreach ($gastosCliente as $g) {
    $id = intval($g["id"]);
    $id_usuario = intval($g["id_usuario"]);
    $descripcion = $conexion->real_escape_string($g["descripcion"]);
    $monto = floatval($g["monto"]);
    $fecha = $conexion->real_escape_string($g["fecha"]);
    $fecha_actualizacion = $conexion->real_escape_string($g["fecha_actualizacion"]);

    $consulta = $conexion->query("SELECT * FROM gastos WHERE id = $id");

    if ($consulta && $consulta->num_rows > 0) {
        $servidor = $consulta->fetch_assoc();
        if ($servidor["fecha_actualizacion"] < $fecha_actualizacion) {
            $conexion->query("UPDATE gastos SET 
                descripcion = '$descripcion', 
                monto = '$monto', 
                fecha = '$fecha', 
                fecha_actualizacion = '$fecha_actualizacion'
                WHERE id = $id");
        }
    } else {
        $conexion->query("INSERT INTO gastos (id, id_viaje, id_usuario, descripcion, monto, fecha, fecha_actualizacion)
            VALUES ($id, $idViaje, $id_usuario, '$descripcion', $monto, '$fecha', '$fecha_actualizacion')");
    }
}

$resultado = $conexion->query("SELECT * FROM gastos WHERE id_viaje = $idViaje");
while ($fila = $resultado->fetch_assoc()) {
    $gastosServidor[] = $fila;
}

echo json_encode(["success" => true, "gastos" => $gastosServidor]);
