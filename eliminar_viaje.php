<?php
header("Content-Type: application/json");
ini_set('display_errors', 1);
error_reporting(E_ALL);

$conexion = new mysqli("localhost", "root", "root", "tripmates_db");

if ($conexion->connect_error) {
    echo json_encode(["success" => false, "error" => "Conexión fallida"]);
    exit;
}

$data = json_decode(file_get_contents("php://input"), true);

if (!$data || !isset($data["id"])) {
    echo json_encode(["success" => false, "error" => "ID no proporcionado"]);
    exit;
}

$id = intval($data["id"]);

$conexion->begin_transaction();

try {
    //$conexion->query("DELETE FROM lista_empaque WHERE id_viaje = $id");
    //$conexion->query("DELETE FROM itinerario WHERE id_viaje = $id");
    //$conexion->query("DELETE FROM gastos WHERE id_viaje = $id");
    $conexion->query("DELETE FROM viajes_usuarios WHERE viaje_id = $id");

    $resultado = $conexion->query("DELETE FROM viajes WHERE id = $id");

    if ($resultado) {
        $conexion->commit();
        echo json_encode(["success" => true]);
    } else {
        throw new Exception("Error al eliminar el viaje");
    }
} catch (Exception $e) {
    $conexion->rollback();
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
?>
