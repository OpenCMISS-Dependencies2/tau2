/******************************************************************************
*   OpenMp Example - Matrix Multiply - C Version
*   Demonstrates a matrix multiply using OpenMP. 
*
*   Modified from here:
*   https://computing.llnl.gov/tutorials/openMP/samples/C/omp_mm.c
*
*   For  PAPI_FP_INS, the exclusive count for the event: 
*   for (null) [OpenMP location: file:matmult.c ]
*   should be  2E+06 / Number of Threads 
******************************************************************************/
#include <stdio.h>
#include <stdlib.h>


#ifdef TAU_MPI
int provided;
#include <mpi.h>
/* NOTE: MPI is just used to spawn multiple copies of the kernel to different ranks.
This is not a parallel implementation */
#endif /* TAU_MPI */


#ifndef MATRIX_SIZE
#define MATRIX_SIZE 1024
#endif

#define MAX_ITERATIONS 10
#define NRA MATRIX_SIZE                 /* number of rows in matrix A */
#define NCA MATRIX_SIZE                 /* number of columns in matrix A */
#define NCB MATRIX_SIZE                 /* number of columns in matrix B */

#define elem(_m,_i,_j) (_m[((_i)*NRA) + (_j)])

double* allocateMatrix(int rows, int cols) {
  int i;
  double *matrix = (double*)malloc((sizeof(double*)) * rows * cols);
  #pragma omp target enter data map(alloc:matrix[0:rows*cols])
  return matrix;
}

void initialize(double *matrix, int rows, int cols) {
  int i,j;
#pragma omp parallel private(i,j) shared(matrix)
  {
    //set_num_threads();
    /*** Initialize matrices ***/
#pragma omp for nowait
    for (i=0; i<rows; i++) {
      for (j=0; j<cols; j++) {
        elem(matrix,i,j)= i+j;
      }
    }
  }
}

void freeMatrix(double* matrix, int rows, int cols) {
  #pragma omp target exit data map(delete:matrix[0:rows*cols])
  free(matrix);
}

/////////////////////////////////////////////////////////////////////
// compute multiplies a and b and returns the result in c using ijk. 
// cols_a and rows_b are the same value
/////////////////////////////////////////////////////////////////////
void compute(double *a, double *b, double *c, int rows_a, int cols_a, int cols_b) {
  int i,j,k;
  printf("%s\n", __func__);
#pragma omp parallel private(i,j,k) shared(a,b,c)
  {
    /*** Do matrix multiply sharing iterations on outer loop ***/
    /*** Display who does which iterations for demonstration purposes ***/
#pragma omp for nowait
    for (i=0; i<rows_a; i++) {
      for(j=0; j<cols_b; j++) {
        for (k=0; k<cols_a; k++) {
          elem(c,i,j) += elem(a,i,k) * elem(b,k,j);
        }
      }
    }
  }   /*** End of parallel region ***/
}

///////////////////////////////////////////////////////////////////////
// compute_interchange multiplies a and b and returns the result in c 
// using ikj loop.  cols_a and rows_b are the same value
///////////////////////////////////////////////////////////////////////
void compute_interchange(double *a, double *b, double *c, int rows_a, int cols_a, int cols_b) {
  int i,j,k;
  printf("%s\n", __func__);
#pragma omp parallel private(i,j,k) shared(a,b,c)
  {
    /*** Do matrix multiply sharing iterations on outer loop ***/
    /*** Display who does which iterations for demonstration purposes ***/
#pragma omp for nowait
    for (i=0; i<rows_a; i++) {
      for (k=0; k<cols_a; k++) {
        for(j=0; j<cols_b; j++) {
          elem(c,i,j) += elem(a,i,k) * elem(b,k,j);
        }
      }
    }
  }   /*** End of parallel region ***/
}

///////////////////////////////////////////////////////////////////////
// compute_interchange multiplies a and b and returns the result in c 
// using ikj loop.  cols_a and rows_b are the same value
///////////////////////////////////////////////////////////////////////
void compute_target(double *a, double *b, double *c, int rows_a, int cols_a, int cols_b) {
    printf("%s\n", __func__);
    int i, j, k;
#pragma omp target data map (to: a[0:rows_a*cols_a],b[0:cols_a*cols_b]) map (tofrom: c[0:rows_a*cols_b])
#pragma omp target
#pragma omp teams distribute parallel for collapse(2) private(i,j,k)
    for (i=0; i<rows_a; i++) {
      for(j=0; j<cols_b; j++) {
        for (k=0; k<cols_a; k++) {
          elem(c,i,j) += elem(a,i,k) * elem(b,k,j);
        }
      }
    }
#if 0
    // This is a *very* simple offload statement, for debugging
    int z = 1;
#pragma omp target map(tofrom: z)
    z = z + 1; // The copy of z on the device has a value of 2.
    printf("After the target region is executed, z = %d\n", z);
#endif
}

double do_work(void) {
  double *a,           /* matrix A to be multiplied */
  *b,           /* matrix B to be multiplied */
  *c;           /* result matrix C */
  a = allocateMatrix(NRA, NCA);
  b = allocateMatrix(NCA, NCB);
  c = allocateMatrix(NRA, NCB);  

/*** Spawn a parallel region explicitly scoping all variables ***/

  initialize(a, NRA, NCA);
  initialize(b, NCA, NCB);
  initialize(c, NRA, NCB);

  // compute(a, b, c, NRA, NCA, NCB);
  // compute_interchange(a, b, c, NRA, NCA, NCB);
  compute_target(a, b, c, NRA, NCA, NCB);

  double result = elem(c,0,1);

  freeMatrix(a, NRA, NCA);
  freeMatrix(b, NCA, NCB);
  freeMatrix(c, NCA, NCB);

  return result;
}

int main (int argc, char *argv[]) 
{
  int i;
#ifdef TAU_MPI
  char name[MPI_MAX_PROCESSOR_NAME];
  int rc,size,len; 
  rc = MPI_Init(&argc, &argv);

  int rank = 0;
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  if (rank == 0) {
      if (provided == MPI_THREAD_MULTIPLE) {
        printf("provided is MPI_THREAD_MULTIPLE\n");
      } else if (provided == MPI_THREAD_FUNNELED) {
        printf("provided is MPI_THREAD_FUNNELED\n");
      }
  }
  MPI_Comm_size(MPI_COMM_WORLD,&size);
  MPI_Get_processor_name(name, &len);
  printf("MPI Rank: %d of %d on %s\n", rank, size, name);fflush(stdout); 
#endif /* TAU_MPI */

  for (i = 0 ; i < MAX_ITERATIONS ; i++) {
    printf("Iteration %d of %d:...\n", i, MAX_ITERATIONS);
    do_work();
  }

  printf ("Done.\n");
#ifdef TAU_MPI
  MPI_Finalize(); 
#endif /* TAU_MPI */

  return 0;
}

