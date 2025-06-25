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

if (!isset($data["id_viaje"]) || !isset($data["itinerario"])) {
    echo json_encode(["success" => false, "error" => "Datos incompletos"]);
    exit;
}

$idViaje = intval($data["id_viaje"]);
$actividadesCliente = $data["itinerario"];
$actividadesServidor = [];

foreach ($actividadesCliente as $a) {
    $id = intval($a["id"]);
    $titulo = $conexion->real_escape_string($a["titulo"]);
    $descripcion = $conexion->real_escape_string($a["descripcion"]);
    $fecha = $conexion->real_escape_string($a["fecha"]);
    $hora = $conexion->real_escape_string($a["hora"]);
    $fecha_actualizacion = $conexion->real_escape_string($a["fecha_actualizacion"]);

    $consulta = $conexion->query("SELECT * FROM itinerario WHERE id = $id");

    if ($consulta && $consulta->num_rows > 0) {
        $servidor = $consulta->fetch_assoc();
        if ($servidor["fecha_actualizacion"] < $fecha_actualizacion) {
            $conexion->query("UPDATE itinerario SET 
                titulo = '$titulo', 
                descripcion = '$descripcion', 
                fecha = '$fecha', 
                hora = '$hora', 
                fecha_actualizacion = '$fecha_actualizacion'
                WHERE id = $id");
        }
    } else {
        $conexion->query("INSERT INTO itinerario (id, id_viaje, titulo, descripcion, fecha, hora, fecha_actualizacion) VALUES (
            $id, $idViaje, '$titulo', '$descripcion', '$fecha', '$hora', '$fecha_actualizacion')");
    }
}

$resultado = $conexion->query("SELECT * FROM itinerario WHERE id_viaje = $idViaje");
while ($fila = $resultado->fetch_assoc()) {
    $actividadesServidor[] = $fila;
}

echo json_encode(["success" => true, "itinerario" => $actividadesServidor]);
?>