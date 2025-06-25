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

if (!isset($data["id_viaje"]) || !isset($data["empaque"])) {
    echo json_encode(["success" => false, "error" => "Datos incompletos"]);
    exit;
}

$idViaje = intval($data["id_viaje"]);
$itemsCliente = $data["empaque"];
$itemsServidor = [];

foreach ($itemsCliente as $item) {
    $id = intval($item["id"]);
    $id_usuario = intval($item["id_usuario"]);
    $texto = $conexion->real_escape_string($item["item"]);
    $marcado = intval($item["marcado"]);
    $fecha_actualizacion = $conexion->real_escape_string($item["fecha_actualizacion"]);

    $consulta = $conexion->query("SELECT * FROM lista_empaque WHERE id = $id");

    if ($consulta && $consulta->num_rows > 0) {
        $servidor = $consulta->fetch_assoc();
        if ($servidor["fecha_actualizacion"] < $fecha_actualizacion) {
            $conexion->query("UPDATE lista_empaque SET 
                item = '$texto', 
                marcado = $marcado, 
                fecha_actualizacion = '$fecha_actualizacion'
                WHERE id = $id");
        }
    } else {
        $conexion->query("INSERT INTO lista_empaque (id, id_viaje, id_usuario, item, marcado, fecha_actualizacion)
            VALUES ($id, $idViaje, $id_usuario, '$texto', $marcado, '$fecha_actualizacion')");
    }
}

$resultado = $conexion->query("SELECT * FROM lista_empaque WHERE id_viaje = $idViaje");
while ($fila = $resultado->fetch_assoc()) {
    $itemsServidor[] = $fila;
}

echo json_encode(["success" => true, "empaque" => $itemsServidor]);
