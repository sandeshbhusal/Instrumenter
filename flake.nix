{
  description = "A very basic flake";

  outputs = { self, nixpkgs }: let
    system = "x86_64-linux";
    pkgs   = nixpkgs.legacyPackages.${system};
  in {
    devShells.${system}.default = pkgs.mkShell {
	buildInputs = [ pkgs.jdk17 pkgs.python311 pkgs.gcc-unwrapped pkgs.gradle pkgs.maven pkgs.just ];
    }; 
  };
}
