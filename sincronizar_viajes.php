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

if (!$data || !isset($data["usuario_id"]) || !isset($data["viajes"])) {
    echo json_encode(["success" => false, "error" => "Datos incompletos"]);
    exit;
}

$idUsuario = $conexion->real_escape_string($data["usuario_id"]);
$viajesCliente = $data["viajes"];
$viajesServidor = [];

// Procesar cada viaje enviado desde la app
foreach ($viajesCliente as $viaje) {
    $id = $conexion->real_escape_string($viaje["id"]);
    $nombre = $conexion->real_escape_string($viaje["nombre"]);
    $destino = $conexion->real_escape_string($viaje["destino"]);
    $fecha_inicio = $conexion->real_escape_string($viaje["fecha_inicio"]);
    $fecha_fin = $conexion->real_escape_string($viaje["fecha_fin"]);
    $creador_id = $conexion->real_escape_string($viaje["creador_id"]);
    $fecha_actualizacion = $conexion->real_escape_string($viaje["fecha_actualizacion"]);

    // Verificar si ya existe en el servidor
    $consulta = $conexion->query("SELECT * FROM viajes WHERE id = '$id'");
    if ($consulta && $consulta->num_rows > 0) {
        $viajeServidor = $consulta->fetch_assoc();
        if ($viajeServidor["fecha_actualizacion"] < $fecha_actualizacion) {
            // El del cliente es más reciente → actualizar en servidor
            $conexion->query("UPDATE viajes SET
                nombre = '$nombre',
                destino = '$destino',
                fecha_inicio = '$fecha_inicio',
                fecha_fin = '$fecha_fin',
                creador_id = '$creador_id',
                fecha_actualizacion = '$fecha_actualizacion'
                WHERE id = '$id'
            ");
        }
    } else {
        // Insertar nuevo
        $conexion->query("INSERT INTO viajes 
            (id, nombre, destino, fecha_inicio, fecha_fin, creador_id, fecha_actualizacion) 
            VALUES ('$id', '$nombre', '$destino', '$fecha_inicio', '$fecha_fin', '$creador_id', '$fecha_actualizacion')");
    }

    // Asegurar relación en viajes_usuarios
    $verificar = $conexion->query("SELECT * FROM viajes_usuarios WHERE usuario_id = '$idUsuario' AND viaje_id = '$id'");
    if ($verificar && $verificar->num_rows == 0) {
        $conexion->query("INSERT INTO viajes_usuarios (usuario_id, viaje_id, rol, fecha_actualizacion)
                          VALUES ('$idUsuario', '$id', 'admin', '$fecha_actualizacion')");
    }
}

// Enviar todos los viajes relacionados al usuario
$resultado = $conexion->query("
    SELECT v.* FROM viajes v
    INNER JOIN viajes_usuarios vu ON v.id = vu.viaje_id
    WHERE vu.usuario_id = '$idUsuario'
");

while ($fila = $resultado->fetch_assoc()) {
    $viajesServidor[] = $fila;
}

echo json_encode(["success" => true, "viajes" => $viajesServidor]);
exit;
?>
