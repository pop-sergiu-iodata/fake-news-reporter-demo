import ast
import operator
from pathlib import Path

from smolagents import tool


WORKSPACE = Path("./workspace").resolve()
WORKSPACE.mkdir(exist_ok=True)


_OPS = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.Pow: operator.pow,
    ast.USub: operator.neg,
}


def _eval(node):
    if isinstance(node, ast.Constant):
        return node.value

    if isinstance(node, ast.BinOp):
        return _OPS[type(node.op)](
            _eval(node.left),
            _eval(node.right)
        )

    if isinstance(node, ast.UnaryOp):
        return _OPS[type(node.op)](
            _eval(node.operand)
        )

    raise ValueError("Operator not allowed")


@tool
def calculate(expression: str) -> float:
    """
    Evaluate a simple arithmetic expression safely.

    Args:
        expression: Arithmetic expression, for example "12 * 5 + 3".
    """
    return _eval(ast.parse(expression, mode="eval").body)


def safe_path(relative_path: str) -> Path:
    path = (WORKSPACE / relative_path).resolve()

    if not path.is_relative_to(WORKSPACE):
        raise ValueError("Path outside workspace is not allowed")

    return path


@tool
def list_workspace_files(relative_path: str = "") -> list[dict]:
    """
    List files and folders inside the local workspace.

    Args:
        relative_path: Folder path inside the workspace. Empty means workspace root.
    """
    path = safe_path(relative_path)

    if not path.exists():
        return []

    if not path.is_dir():
        raise NotADirectoryError(relative_path)

    return [
        {
            "name": item.name,
            "type": "directory" if item.is_dir() else "file",
            "size_bytes": item.stat().st_size,
        }
        for item in path.iterdir()
    ]


@tool
def read_workspace_file(relative_path: str, max_chars: int = 5000) -> str:
    """
    Read a text file from the local workspace.

    Args:
        relative_path: File path inside the workspace.
        max_chars: Maximum number of characters to return.
    """
    path = safe_path(relative_path)

    if not path.exists():
        raise FileNotFoundError(relative_path)

    return path.read_text(encoding="utf-8")[:max_chars]


@tool
def write_workspace_file(relative_path: str, content: str) -> str:
    """
    Create a new text file inside the local workspace. Fails if it already exists.

    Args:
        relative_path: File path inside the workspace.
        content: Text content to write.
    """
    path = safe_path(relative_path)

    if path.exists():
        raise FileExistsError(f"File already exists: {relative_path}")

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")

    return f"Created {relative_path} ({len(content)} chars)"