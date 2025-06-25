<?php
header("Content-Type: application/json; charset=utf-8");
ini_set('display_errors', 1);
error_reporting(E_ALL);

$response = [];

try {
    $conexion = new mysqli("localhost", "root", "root", "tripmates_db");
    if ($conexion->connect_error) {
        throw new Exception("Error de conexión: " . $conexion->connect_error);
    }

    $inputJSON = file_get_contents("php://input");
    file_put_contents("debug_login.json", $inputJSON); // log de entrada

    $data = json_decode($inputJSON, true);
    if (!$data || !isset($data["correo"]) || !isset($data["contraseña"])) {
        throw new Exception("Faltan campos requeridos.");
    }

    $correo = $data["correo"];
    $contrasena = $data["contraseña"];

    $sql = "SELECT * FROM usuarios WHERE correo = ? AND contrasena = ?";
    $stmt = $conexion->prepare($sql);
    if (!$stmt) {
        throw new Exception("Error al preparar consulta: " . $conexion->error);
    }

    $stmt->bind_param("ss", $correo, $contrasena);
    $stmt->execute();
    $result = $stmt->get_result();
    if ($result === false) {
        throw new Exception("Error al obtener resultado.");
    }

    if ($result->num_rows === 1) {
        $usuario = $result->fetch_assoc();
        $response["success"] = true;
        $response["usuario"] = $usuario;
    } else {
        $response["success"] = false;
        $response["error"] = "Credenciales incorrectas";
    }

} catch (Exception $e) {
    $response["success"] = false;
    $response["error"] = $e->getMessage();
}

// SIEMPRE termina con JSON válido
echo json_encode($response);
exit;
?>
