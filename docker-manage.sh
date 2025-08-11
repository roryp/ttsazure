#!/bin/bash
# Docker management script for TTS Azure App

set -e

CONTAINER_NAME="tts-app"
IMAGE_NAME="tts-azure-app"

case "$1" in
    build)
        echo "🔨 Building Docker image..."
        docker build -t $IMAGE_NAME .
        echo "✅ Build complete!"
        ;;
    run)
        echo "🚀 Starting container with environment variables..."
        # Stop and remove existing container if it exists
        docker stop $CONTAINER_NAME 2>/dev/null || true
        docker rm $CONTAINER_NAME 2>/dev/null || true
        
        # Run new container
        docker run -d \
            --name $CONTAINER_NAME \
            --env-file .env \
            -p 8080:8080 \
            $IMAGE_NAME
        
        echo "✅ Container started!"
        echo "🌐 Application available at: http://localhost:8080"
        ;;
    stop)
        echo "🛑 Stopping container..."
        docker stop $CONTAINER_NAME
        echo "✅ Container stopped!"
        ;;
    logs)
        echo "📋 Showing container logs..."
        docker logs -f $CONTAINER_NAME
        ;;
    status)
        echo "📊 Container status:"
        docker ps -a --filter name=$CONTAINER_NAME
        ;;
    restart)
        echo "🔄 Restarting container..."
        docker restart $CONTAINER_NAME
        echo "✅ Container restarted!"
        ;;
    clean)
        echo "🧹 Cleaning up..."
        docker stop $CONTAINER_NAME 2>/dev/null || true
        docker rm $CONTAINER_NAME 2>/dev/null || true
        docker rmi $IMAGE_NAME 2>/dev/null || true
        echo "✅ Cleanup complete!"
        ;;
    *)
        echo "🚀 TTS Azure App Docker Management"
        echo ""
        echo "Usage: $0 {build|run|stop|logs|status|restart|clean}"
        echo ""
        echo "Commands:"
        echo "  build   - Build the Docker image"
        echo "  run     - Run the container with .env file"
        echo "  stop    - Stop the running container"
        echo "  logs    - Show container logs (follow mode)"
        echo "  status  - Show container status"
        echo "  restart - Restart the container"
        echo "  clean   - Stop and remove container and image"
        echo ""
        echo "Examples:"
        echo "  $0 build    # Build the image"
        echo "  $0 run      # Start the application"
        echo "  $0 logs     # Watch the logs"
        exit 1
        ;;
esac
